# 03 - Authentication and Redis

## Authentication flow

```text
Register
  -> find tenant by code
  -> BCrypt hash password
  -> insert app_user

Login
  -> Redis read failed-login counter
  -> find tenant and user
  -> BCrypt compare password
  -> issue JWT
  -> return Bearer token
```

## Password storage

Never store a plaintext password.

```text
Password123!
  -> BCrypt
  -> $2a$10$...
```

BCrypt is deliberately slow because it includes a work factor. This makes offline password cracking more expensive.

## JWT

The JWT contains:

- `sub`: user ID
- `tenantId`: tenant ID
- `username`: username
- `role`: user role
- `jti`: unique token ID
- `iat`: issued time
- `exp`: expiration time

JWT is signed, not encrypted. Do not put passwords or sensitive data in JWT claims.

## Redis rate limiting

Failed logins increment:

```text
smartdesk:auth:login-failure:{tenantCode}:{username}:{clientIp}
```

The first failure sets a TTL. Five failures within ten minutes cause HTTP 429.

The read and increment operations are separate:
- read checks whether the request is allowed;
- a Lua script increments and sets TTL atomically after a failed login.

## Token blacklist

JWT is stateless, so logout cannot delete the token from the client only. The server stores the JWT `jti` in Redis until its original expiration time.

Every authenticated request checks:

```text
Authorization: Bearer token
  -> verify signature and expiration
  -> check Redis blacklist by jti
  -> create SecurityContext authentication
```

## Startup dependency behavior

Redis is used for rate limiting and blacklist checks. The current implementation fails open when Redis is unavailable and logs a warning. Authentication and durable business data continue to work.

In production, decide explicitly whether each Redis dependency should fail open or fail closed.

## Tenant bootstrap

A fresh database has no tenant and therefore no administrator. The bootstrap endpoint solves this onboarding problem:

```text
POST /api/v1/bootstrap/tenant
```

It succeeds only when `tenant` contains zero rows. After the first tenant exists, additional tenants must be created by an authenticated administrator.

The first user registered under a tenant receives `ADMIN`. Later users receive `USER`.

Tenant administration endpoints require `ROLE_ADMIN`.