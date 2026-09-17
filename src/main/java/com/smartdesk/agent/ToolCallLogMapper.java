package com.smartdesk.agent;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ToolCallLogMapper {

    int insert(ToolCallLogEntity log);
}