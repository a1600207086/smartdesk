package com.smartdesk.agent;

public interface AgentChatModel {

    void stream(AgentModelRequest request, AgentChunkConsumer consumer) throws Exception;
}