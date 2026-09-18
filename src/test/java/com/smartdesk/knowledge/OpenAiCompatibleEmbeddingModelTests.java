package com.smartdesk.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenAiCompatibleEmbeddingModelTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void shouldEmbedBatchAndUseBearerToken() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("""
                {
                  "data": [
                    {"index": 0, "embedding": [0.1, 0.2, 0.3]},
                    {"index": 1, "embedding": [0.4, 0.5, 0.6]}
                  ]
                }
                """);
        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        OpenAiCompatibleEmbeddingModel model = new OpenAiCompatibleEmbeddingModel(
                properties(3),
                objectMapper,
                httpClient
        );

        List<float[]> vectors = model.embedAll(List.of("退款政策", "物流规则"));

        assertThat(vectors).hasSize(2);
        assertThat(vectors.get(0)).containsExactly(0.1f, 0.2f, 0.3f);
        assertThat(vectors.get(1)).containsExactly(0.4f, 0.5f, 0.6f);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any());
        HttpRequest request = requestCaptor.getValue();
        assertThat(request.uri().toString()).isEqualTo("https://example.com/v1/embeddings");
        assertThat(request.headers().firstValue("Authorization")).contains("Bearer embedding-key");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRejectDimensionMismatch() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("""
                {"data":[{"index":0,"embedding":[0.1,0.2]}]}
                """);
        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        OpenAiCompatibleEmbeddingModel model = new OpenAiCompatibleEmbeddingModel(
                properties(3),
                objectMapper,
                httpClient
        );

        assertThatThrownBy(() -> model.embed("test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dimension mismatch");
    }

    private EmbeddingProperties properties(int dimensions) {
        return new EmbeddingProperties(
                true,
                "https://example.com/v1/",
                "embedding-key",
                "test-embedding-model",
                dimensions,
                Duration.ofSeconds(5),
                32
        );
    }
}