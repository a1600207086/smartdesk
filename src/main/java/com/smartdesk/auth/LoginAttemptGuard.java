package com.smartdesk.auth;

public interface LoginAttemptGuard {

    void assertAllowed(String key);

    void recordFailure(String key);

    void clear(String key);
}