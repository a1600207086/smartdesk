package com.smartdesk.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenAiCompatibleAgentChatModelTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void shouldStreamDeltaContentAndUseBearerToken() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<Stream<String>> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(Stream.of(
                "data: {\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}",
                "",
                "data: {\"choices\":[{\"delta\":{\"content\":\" world\"}}]}",
                "data: [DONE]"
        ));
        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        OpenAiCompatibleAgentChatModel model = new OpenAiCompatibleAgentChatModel(
                properties(),
                objectMapper,
                httpClient
        );

        List<String> chunks = new ArrayList<>();
        model.stream(new AgentModelRequest(List.of(
                new AgentModelMessage("system", "You are helpful."),
                new AgentModelMessage("user", "Hello")
        )), chunks::add);

        assertThat(chunks).containsExactly("Hello", " world");

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any());
        HttpRequest request = requestCaptor.getValue();
        assertThat(request.uri().toString()).isEqualTo("https://example.com/v1/chat/completions");
        assertThat(request.headers().firstValue("Authorization")).contains("Bearer test-key");
        assertThat(request.headers().firstValue("Accept")).contains("text/event-stream");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRejectNonSuccessfulResponse() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<Stream<String>> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(401);
        when(response.body()).thenReturn(Stream.of("{\"error\":\"invalid api key\"}"));
        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        OpenAiCompatibleAgentChatModel model = new OpenAiCompatibleAgentChatModel(
                properties(),
                objectMapper,
                httpClient
        );

        assertThatThrownBy(() -> model.stream(
                new AgentModelRequest(List.of(new AgentModelMessage("user", "Hello"))),
                chunk -> {
                }
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP 401")
                .hasMessageContaining("invalid api key");
    }

    private LlmProperties properties() {
        return new LlmProperties(
                true,
                "https://example.com/v1/",
                "test-key",
                "test-model",
                Duration.ofSeconds(5),
                0.2,
                128
        );
    }
}
