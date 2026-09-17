package com.smartdesk.auth;

import com.smartdesk.common.error.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisLoginAttemptGuard implements LoginAttemptGuard {

    private static final Logger log = LoggerFactory.getLogger(RedisLoginAttemptGuard.class);
    private static final String KEY_PREFIX = "smartdesk:auth:login-failure:";
    private static final DefaultRedisScript<Long> RECORD_FAILURE_SCRIPT = new DefaultRedisScript<>(
            """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final LoginRateLimitProperties properties;

    public RedisLoginAttemptGuard(
            StringRedisTemplate redisTemplate,
            LoginRateLimitProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public void assertAllowed(String key) {
        try {
            String value = redisTemplate.opsForValue().get(redisKey(key));
            long count = value == null ? 0 : Long.parseLong(value);
            if (count >= properties.maxAttempts()) {
                throw new TooManyRequestsException("登录失败次数过多，请稍后再试");
            }
        } catch (TooManyRequestsException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Redis unavailable while checking login rate limit; allowing request", exception);
        }
    }

    @Override
    public void recordFailure(String key) {
        try {
            long ttlSeconds = Math.max(1, properties.window().getSeconds());
            redisTemplate.execute(
                    RECORD_FAILURE_SCRIPT,
                    List.of(redisKey(key)),
                    Long.toString(ttlSeconds)
            );
        } catch (RuntimeException exception) {
            log.warn("Redis unavailable while recording login failure", exception);
        }
    }

    @Override
    public void clear(String key) {
        try {
            redisTemplate.delete(redisKey(key));
        } catch (RuntimeException exception) {
            log.warn("Redis unavailable while clearing login failures", exception);
        }
    }

    private String redisKey(String key) {
        return KEY_PREFIX + key;
    }
}