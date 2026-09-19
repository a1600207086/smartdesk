package com.smartdesk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdesk.auth.LoginAttemptGuard;
import com.smartdesk.auth.TokenBlacklistService;
import com.smartdesk.conversation.ConversationMemoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tickets;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupportTicketApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private LoginAttemptGuard loginAttemptGuard;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private ConversationMemoryService conversationMemoryService;

    @BeforeEach
    void setUpConversationMemory() {
        when(conversationMemoryService.getRecent(anyLong(), anyInt())).thenReturn(List.of());
    }

    @Test
    void shouldCreateTicketFromAgentAndManageWorkflow() throws Exception {
        bootstrapTenant();
        String adminToken = registerAndLogin("admin");
        String memberToken = registerAndLogin("member");
        long conversationId = createConversation(memberToken);

        String stream = chat(memberToken, conversationId, "退款问题一直没解决，请帮我转人工客服");
        assertThat(stream).contains("\"toolName\":\"createSupportTicket\"");
        assertThat(stream).contains("人工客服工单");

        chat(memberToken, conversationId, "请再次帮我转人工");
        Integer ticketCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM support_ticket WHERE conversation_id = ?",
                Integer.class,
                conversationId
        );
        assertThat(ticketCount).isEqualTo(1);

        MvcResult memberList = mockMvc.perform(get("/api/v1/tickets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.data[0].status").value("OPEN"))
                .andReturn();
        JsonNode ticket = objectMapper.readTree(memberList.getResponse().getContentAsString())
                .path("data").path(0);
        long ticketId = ticket.path("id").asLong();
        assertThat(ticket.path("ticketNo").asText()).startsWith("T-");

        mockMvc.perform(patch("/api/v1/tickets/{id}/status", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());

        updateStatus(adminToken, ticketId, "IN_PROGRESS", "IN_PROGRESS");
        updateStatus(adminToken, ticketId, "RESOLVED", "RESOLVED");
        updateStatus(adminToken, ticketId, "CLOSED", "CLOSED");

        mockMvc.perform(patch("/api/v1/tickets/{id}/status", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/v1/tickets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(ticketId))
                .andExpect(jsonPath("$.data[0].status").value("CLOSED"));

        mockMvc.perform(get("/api/v1/tickets/metrics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.openCount").value(0))
                .andExpect(jsonPath("$.data.inProgressCount").value(0))
                .andExpect(jsonPath("$.data.resolvedCount").value(0))
                .andExpect(jsonPath("$.data.closedCount").value(1))
                .andExpect(jsonPath("$.data.urgentOpenCount").value(0))
                .andExpect(jsonPath("$.data.averageResolutionHours").isNumber());

        mockMvc.perform(get("/api/v1/tickets/metrics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    private void updateStatus(
            String token,
            long ticketId,
            String requestedStatus,
            String expectedStatus
    ) throws Exception {
        mockMvc.perform(patch("/api/v1/tickets/{id}/status", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + requestedStatus + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(expectedStatus));
    }

    private void bootstrapTenant() throws Exception {
        mockMvc.perform(post("/api/v1/bootstrap/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ticketco\",\"name\":\"Ticket Tenant\"}"))
                .andExpect(status().isCreated());
    }

    private String registerAndLogin(String username) throws Exception {
        String registerRequest = """
                {
                  "tenantCode":"ticketco",
                  "username":"%s",
                  "displayName":"Ticket User",
                  "password":"Password123!"
                }
                """.formatted(username);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest))
                .andExpect(status().isCreated());

        String loginRequest = """
                {"tenantCode":"ticketco","username":"%s","password":"Password123!"}
                """.formatted(username);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private long createConversation(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/conversations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Ticket chat\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private String chat(String token, long conversationId, String message) throws Exception {
        MvcResult asyncResult = mockMvc.perform(post(
                        "/api/v1/conversations/{id}/chat", conversationId
                )
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"message\":\"" + message + "\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        return mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:done")))
                .andReturn().getResponse().getContentAsString();
    }
}
