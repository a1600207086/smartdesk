package com.smartdesk.tenant;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TenantMapper {

    int insert(TenantEntity tenant);

    TenantEntity findById(@Param("id") Long id);

    TenantEntity findByCode(@Param("code") String code);

    List<TenantEntity> findAll();

    int countAll();

    int countByCode(@Param("code") String code);
}