package com.smartdesk.tenant;

import com.smartdesk.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bootstrap")
public class BootstrapController {

    private final TenantService tenantService;

    public BootstrapController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping("/tenant")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TenantResponse> createBootstrapTenant(
            @Valid @RequestBody CreateTenantRequest request
    ) {
        return ApiResponse.success(tenantService.createBootstrapTenant(request));
    }
}