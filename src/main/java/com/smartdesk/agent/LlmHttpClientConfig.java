package com.smartdesk.agent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class LlmHttpClientConfig {

    @Bean(name = "llmHttpClient")
    @ConditionalOnProperty(prefix = "smartdesk.llm", name = "enabled", havingValue = "true")
    public HttpClient llmHttpClient(LlmProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .build();
    }
}