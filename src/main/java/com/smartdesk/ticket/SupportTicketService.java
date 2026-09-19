package com.smartdesk.ticket;

import com.smartdesk.auth.AuthenticatedUser;
import com.smartdesk.auth.UserRole;
import com.smartdesk.common.error.NotFoundException;
import com.smartdesk.conversation.ConversationEntity;
import com.smartdesk.conversation.ConversationMapper;
import com.smartdesk.conversation.ConversationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SupportTicketService {

    private static final DateTimeFormatter TICKET_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SupportTicketMapper ticketMapper;
    private final ConversationMapper conversationMapper;
    private final ConversationService conversationService;

    public SupportTicketService(
            SupportTicketMapper ticketMapper,
            ConversationMapper conversationMapper,
            ConversationService conversationService
    ) {
        this.ticketMapper = ticketMapper;
        this.conversationMapper = conversationMapper;
        this.conversationService = conversationService;
    }

    @Transactional
    public SupportTicketResponse create(
            AuthenticatedUser user,
            CreateSupportTicketRequest request
    ) {
        if (request.conversationId() != null) {
            conversationService.requireOwnedConversation(request.conversationId(), user);
            SupportTicketEntity active = ticketMapper.findActiveByConversationId(
                    user.tenantId(), user.userId(), request.conversationId()
            );
            if (active != null) {
                return SupportTicketResponse.from(active);
            }
        }
        return SupportTicketResponse.from(insert(
                user.tenantId(),
                user.userId(),
                request.conversationId(),
                request.subject(),
                request.description(),
                request.priority() == null ? TicketPriority.NORMAL : request.priority()
        ));
    }

    @Transactional
    public SupportTicketResponse createFromAgent(
            Long tenantId,
            Long userId,
            Long conversationId,
            String reason
    ) {
        ConversationEntity conversation = conversationMapper.findById(conversationId);
        if (conversation == null
                || !conversation.getTenantId().equals(tenantId)
                || !conversation.getUserId().equals(userId)) {
            throw new NotFoundException("会话不存在: " + conversationId);
        }

        SupportTicketEntity active = ticketMapper.findActiveByConversationId(
                tenantId, userId, conversationId
        );
        if (active != null) {
            return SupportTicketResponse.from(active);
        }

        String normalizedReason = normalize(reason, "用户请求人工客服", 5000);
        return SupportTicketResponse.from(insert(
                tenantId,
                userId,
                conversationId,
                "Agent 转人工请求",
                normalizedReason,
                TicketPriority.HIGH
        ));
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> findAll(AuthenticatedUser user) {
        List<SupportTicketEntity> tickets = isStaff(user.role())
                ? ticketMapper.findAllByTenantId(user.tenantId())
                : ticketMapper.findAllByTenantIdAndUserId(user.tenantId(), user.userId());
        return tickets.stream().map(SupportTicketResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse findById(Long ticketId, AuthenticatedUser user) {
        return SupportTicketResponse.from(requireVisibleTicket(ticketId, user));
    }

    @Transactional(readOnly = true)
    public TicketMetricsResponse summarize(AuthenticatedUser user) {
        if (!isStaff(user.role())) {
            throw new NotFoundException("工单统计不存在");
        }
        return TicketMetricsResponse.from(ticketMapper.summarizeByTenantId(user.tenantId()));
    }

    @Transactional
    public SupportTicketResponse updateStatus(
            Long ticketId,
            TicketStatus nextStatus,
            AuthenticatedUser user
    ) {
        if (!isStaff(user.role())) {
            throw new NotFoundException("工单不存在: " + ticketId);
        }
        SupportTicketEntity ticket = requireVisibleTicket(ticketId, user);
        validateTransition(ticket.getStatus(), nextStatus);
        if (ticket.getStatus() == nextStatus) {
            return SupportTicketResponse.from(ticket);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime resolvedAt = switch (nextStatus) {
            case RESOLVED, CLOSED -> ticket.getResolvedAt() == null ? now : ticket.getResolvedAt();
            case OPEN, IN_PROGRESS -> null;
        };
        ticketMapper.updateStatus(ticketId, user.tenantId(), nextStatus, now, resolvedAt);
        ticket.setStatus(nextStatus);
        ticket.setUpdatedAt(now);
        ticket.setResolvedAt(resolvedAt);
        return SupportTicketResponse.from(ticket);
    }

    private SupportTicketEntity insert(
            Long tenantId,
            Long userId,
            Long conversationId,
            String subject,
            String description,
            TicketPriority priority
    ) {
        LocalDateTime now = LocalDateTime.now();
        SupportTicketEntity ticket = new SupportTicketEntity();
        ticket.setTicketNo("T-" + TICKET_DATE.format(now) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        ticket.setTenantId(tenantId);
        ticket.setUserId(userId);
        ticket.setConversationId(conversationId);
        ticket.setSubject(normalize(subject, "售后支持请求", 255));
        ticket.setDescription(normalize(description, "用户未提供问题描述", 5000));
        ticket.setPriority(priority);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        ticketMapper.insert(ticket);
        return ticket;
    }

    private SupportTicketEntity requireVisibleTicket(Long ticketId, AuthenticatedUser user) {
        SupportTicketEntity ticket = ticketMapper.findByIdAndTenantId(ticketId, user.tenantId());
        if (ticket == null || (!isStaff(user.role()) && !ticket.getUserId().equals(user.userId()))) {
            throw new NotFoundException("工单不存在: " + ticketId);
        }
        return ticket;
    }

    private void validateTransition(TicketStatus current, TicketStatus next) {
        if (current == next) {
            return;
        }
        Set<TicketStatus> allowed = switch (current) {
            case OPEN -> EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED);
            case IN_PROGRESS -> EnumSet.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);
            case RESOLVED -> EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED);
            case CLOSED -> EnumSet.noneOf(TicketStatus.class);
        };
        if (!allowed.contains(next)) {
            throw new IllegalArgumentException(
                    "不允许将工单状态从 " + current + " 更新为 " + next
            );
        }
    }

    private boolean isStaff(UserRole role) {
        return role == UserRole.ADMIN || role == UserRole.AGENT;
    }

    private String normalize(String value, String fallback, int maxLength) {
        String normalized = value == null || value.isBlank() ? fallback : value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
