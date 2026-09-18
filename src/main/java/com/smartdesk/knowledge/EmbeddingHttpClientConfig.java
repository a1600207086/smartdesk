package com.smartdesk.knowledge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class EmbeddingHttpClientConfig {

    @Bean(name = "embeddingHttpClient")
    @ConditionalOnProperty(prefix = "smartdesk.embedding", name = "enabled", havingValue = "true")
    public HttpClient embeddingHttpClient(EmbeddingProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .build();
    }
}