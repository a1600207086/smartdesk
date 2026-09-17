package com.smartdesk.tenant;

import com.smartdesk.common.error.ConflictException;
import com.smartdesk.common.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class TenantService {

    private final TenantMapper tenantMapper;

    public TenantService(TenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    @Transactional
    public TenantResponse create(CreateTenantRequest request) {
        String code = request.code().trim().toLowerCase(Locale.ROOT);
        if (tenantMapper.countByCode(code) > 0) {
            throw new ConflictException("租户编码已存在: " + code);
        }

        LocalDateTime now = LocalDateTime.now();
        TenantEntity tenant = new TenantEntity();
        tenant.setCode(code);
        tenant.setName(request.name().trim());
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setCreatedAt(now);
        tenant.setUpdatedAt(now);

        tenantMapper.insert(tenant);
        return TenantResponse.from(tenant);
    }

    @Transactional(readOnly = true)
    public TenantResponse findById(Long id) {
        TenantEntity tenant = tenantMapper.findById(id);
        if (tenant == null) {
            throw new NotFoundException("租户不存在: " + id);
        }
        return TenantResponse.from(tenant);
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> findAll() {
        return tenantMapper.findAll()
                .stream()
                .map(TenantResponse::from)
                .toList();
    }
}
