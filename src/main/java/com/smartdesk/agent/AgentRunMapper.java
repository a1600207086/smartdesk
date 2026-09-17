package com.smartdesk.agent;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface AgentRunMapper {

    int insert(AgentRunEntity run);

    int updateRoute(@Param("id") Long id, @Param("route") AgentRoute route);

    int complete(@Param("id") Long id, @Param("finishedAt") LocalDateTime finishedAt);

    int fail(
            @Param("id") Long id,
            @Param("errorMessage") String errorMessage,
            @Param("finishedAt") LocalDateTime finishedAt
    );
}