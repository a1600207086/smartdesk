package com.smartdesk.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String KEY_PREFIX = "smartdesk:auth:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isBlacklisted(String jwtId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jwtId));
        } catch (RuntimeException exception) {
            log.warn("Redis unavailable while checking token blacklist; treating token as active", exception);
            return false;
        }
    }

    public void blacklist(String jwtId, Duration ttl) {
        if (jwtId == null || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + jwtId, "1", ttl);
        } catch (RuntimeException exception) {
            log.warn("Redis unavailable while blacklisting token", exception);
        }
    }
}