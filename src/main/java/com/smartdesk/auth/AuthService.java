package com.smartdesk.auth;

import com.smartdesk.common.error.ConflictException;
import com.smartdesk.common.error.NotFoundException;
import com.smartdesk.common.error.UnauthorizedException;
import com.smartdesk.tenant.TenantEntity;
import com.smartdesk.tenant.TenantMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AuthService {

    private final TenantMapper tenantMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptGuard loginAttemptGuard;

    public AuthService(
            TenantMapper tenantMapper,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LoginAttemptGuard loginAttemptGuard
    ) {
        this.tenantMapper = tenantMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttemptGuard = loginAttemptGuard;
    }

    @Transactional
    public UserProfile register(RegisterRequest request) {
        String tenantCode = normalizeCode(request.tenantCode());
        String username = normalizeUsername(request.username());

        TenantEntity tenant = tenantMapper.findByCode(tenantCode);
        if (tenant == null) {
            throw new NotFoundException("租户不存在: " + tenantCode);
        }
        if (userMapper.countByTenantIdAndUsername(tenant.getId(), username) > 0) {
            throw new ConflictException("用户名已存在: " + username);
        }

        LocalDateTime now = LocalDateTime.now();
        UserEntity user = new UserEntity();
        user.setTenantId(tenant.getId());
        user.setUsername(username);
        user.setDisplayName(request.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(AppUserStatus.ACTIVE);
        user.setRole(userMapper.countByTenantId(tenant.getId()) == 0 ? UserRole.ADMIN : UserRole.USER);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        userMapper.insert(user);
        return UserProfile.from(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String clientIp) {
        String tenantCode = normalizeCode(request.tenantCode());
        String username = normalizeUsername(request.username());
        String rateLimitKey = tenantCode + ":" + username + ":" + clientIp;

        loginAttemptGuard.assertAllowed(rateLimitKey);

        TenantEntity tenant = tenantMapper.findByCode(tenantCode);
        UserEntity user = tenant == null
                ? null
                : userMapper.findByTenantIdAndUsername(tenant.getId(), username);

        if (user == null
                || user.getStatus() != AppUserStatus.ACTIVE
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptGuard.recordFailure(rateLimitKey);
            throw new UnauthorizedException("租户、用户名或密码错误");
        }

        loginAttemptGuard.clear(rateLimitKey);

        LocalDateTime now = LocalDateTime.now();
        userMapper.updateLastLogin(user.getId(), now);
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);

        JwtService.IssuedToken token = jwtService.issue(user);
        return new LoginResponse(
                token.value(),
                "Bearer",
                token.expiresAt(),
                UserProfile.from(user)
        );
    }

    @Transactional(readOnly = true)
    public UserProfile getProfile(AuthenticatedUser authenticatedUser) {
        UserEntity user = userMapper.findById(authenticatedUser.userId());
        if (user == null || !user.getTenantId().equals(authenticatedUser.tenantId())) {
            throw new UnauthorizedException("用户登录状态无效");
        }
        return UserProfile.from(user);
    }

    private static String normalizeCode(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeUsername(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
