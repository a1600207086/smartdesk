package com.smartdesk.auth;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    int insert(UserEntity user);

    UserEntity findById(@Param("id") Long id);

    UserEntity findByTenantIdAndUsername(
            @Param("tenantId") Long tenantId,
            @Param("username") String username
    );

    int countByTenantId(@Param("tenantId") Long tenantId);

    int countByTenantIdAndUsername(
            @Param("tenantId") Long tenantId,
            @Param("username") String username
    );

    int updateLastLogin(
            @Param("id") Long id,
            @Param("lastLoginAt") java.time.LocalDateTime lastLoginAt
    );
}