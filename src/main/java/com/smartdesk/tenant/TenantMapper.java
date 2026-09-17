package com.smartdesk.tenant;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TenantMapper {

    int insert(TenantEntity tenant);

    TenantEntity findById(@Param("id") Long id);

    List<TenantEntity> findAll();

    int countByCode(@Param("code") String code);
}
