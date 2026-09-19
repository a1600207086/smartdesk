package com.smartdesk.agent;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ToolCallLogMapper {

    int insert(ToolCallLogEntity log);

    List<ToolCallLogEntity> findAllByAgentRunId(@Param("agentRunId") Long agentRunId);
}
