package com.smartdesk.tenant;

import com.smartdesk.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TenantResponse> create(@Valid @RequestBody CreateTenantRequest request) {
        return ApiResponse.success(tenantService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TenantResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(tenantService.findById(id));
    }

    @GetMapping
    public ApiResponse<List<TenantResponse>> findAll() {
        return ApiResponse.success(tenantService.findAll());
    }
}
