package com.smartdesk.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingModelSelectionTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(KnowledgeProperties.class, () -> new KnowledgeProperties(
                    800,
                    120,
                    256,
                    4,
                    20,
                    0.05,
                    Duration.ofMinutes(10),
                    10 * 1024 * 1024,
                    1_000_000
            ))
            .withUserConfiguration(HashEmbeddingModel.class);

    @Test
    void shouldRegisterHashModelWhenEmbeddingIsDisabled() {
        contextRunner
                .withPropertyValues("smartdesk.embedding.enabled=false")
                .run(context -> assertThat(context).hasSingleBean(HashEmbeddingModel.class));
    }

    @Test
    void shouldNotRegisterHashModelWhenEmbeddingIsEnabled() {
        contextRunner
                .withPropertyValues("smartdesk.embedding.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(HashEmbeddingModel.class));
    }
}
