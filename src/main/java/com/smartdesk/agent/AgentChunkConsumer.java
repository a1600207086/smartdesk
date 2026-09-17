package com.smartdesk.agent;

import java.io.IOException;

@FunctionalInterface
public interface AgentChunkConsumer {

    void accept(String chunk) throws IOException;
}