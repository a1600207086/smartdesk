# 03 - 认证与 Redis

## 认证流程

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

## 密码存储

绝不能保存明文密码。

```text
Password123!
  -> BCrypt
  -> $2a$10$...
```

BCrypt 会通过工作因子降低计算速度，从而提高离线破解密码的成本。

## JWT 令牌

JWT 包含以下声明：

- `sub`: user ID
- `tenantId`: tenant ID
- `username`: username
- `role`: user role
- `jti`: unique token ID
- `iat`: issued time
- `exp`: expiration time

JWT 只有签名，并未加密。不要在 JWT 声明中放入密码或敏感数据。

## Redis 登录限流

Failed logins increment:

```text
smartdesk:auth:login-failure:{tenantCode}:{username}:{clientIp}
```

第一次失败会设置 TTL，十分钟内失败五次会返回 HTTP 429。

读取和递增操作分开执行：
- 读取操作检查当前请求是否允许继续；
- 登录失败后由 Lua 脚本原子地递增计数并设置 TTL。

## Token 黑名单

JWT 是无状态令牌，仅由客户端删除 Token 无法真正实现退出登录。服务端会将 JWT 的 `jti` 保存到 Redis，直到令牌原本的过期时间。

Every authenticated request checks:

```text
Authorization: Bearer token
  -> verify signature and expiration
  -> check Redis blacklist by jti
  -> create SecurityContext authentication
```

## 启动依赖行为

Redis 用于登录限流和黑名单检查。当前实现中 Redis 不可用时会记录警告并降级放行，认证和持久化业务数据仍可继续工作。

在生产环境中，应明确每个 Redis 依赖是采用故障放行还是故障拒绝。

## 租户初始化

新数据库没有租户，也就没有管理员。初始化接口用于解决首次接入问题：

```text
POST /api/v1/bootstrap/tenant
```

只有 `tenant` 表为空时接口才会成功。第一个租户创建后，新增租户必须由已认证的管理员创建。

租户下注册的第一个用户获得 `ADMIN` 角色，后续用户获得 `USER` 角色。

租户管理接口要求具备 `ROLE_ADMIN` 权限。
