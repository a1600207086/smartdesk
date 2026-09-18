package com.smartdesk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdesk.auth.LoginAttemptGuard;
import com.smartdesk.auth.TokenBlacklistService;
import com.smartdesk.knowledge.KnowledgeSearchCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:knowledgeasync;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KnowledgeAsyncApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoginAttemptGuard loginAttemptGuard;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private KnowledgeSearchCache knowledgeSearchCache;

    @BeforeEach
    void setUpKnowledgeCache() {
        when(knowledgeSearchCache.currentVersion(anyLong())).thenReturn("async-test");
        when(knowledgeSearchCache.get(anyLong(), anyString(), anyInt(), anyString()))
                .thenReturn(List.of());
    }

    @Test
    void shouldProcessDocumentAsynchronouslyAndRetry() throws Exception {
        ensureTenant("asyncco");
        String token = registerAndLogin("asyncco", "asyncadmin");

        String request = """
                {
                  "title": "异步退款政策",
                  "content": "异步退款政策：审核通过后三个工作日到账。",
                  "sourceUri": "internal://policy/async-refund"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/knowledge/documents/text/async")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andReturn();

        long documentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        JsonNode readyDocument = waitForStatus(token, documentId, "READY");
        assertThat(readyDocument.path("chunkCount").asInt()).isGreaterThan(0);

        String search = """
                {
                  "query": "异步退款多久到账？",
                  "topK": 3
                }
                """;

        mockMvc.perform(post("/api/v1/knowledge/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(search))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].documentTitle").value("异步退款政策"));

        mockMvc.perform(post("/api/v1/knowledge/documents/{id}/retry", documentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));

        waitForStatus(token, documentId, "READY");
    }

    private JsonNode waitForStatus(String token, long documentId, String expectedStatus) throws Exception {
        JsonNode document = null;
        for (int attempt = 0; attempt < 50; attempt++) {
            MvcResult result = mockMvc.perform(get(
                            "/api/v1/knowledge/documents/{id}",
                            documentId
                    )
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();

            document = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
            if (expectedStatus.equals(document.path("status").asText())) {
                return document;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Document did not reach status " + expectedStatus + ": " + document);
    }

    private void ensureTenant(String tenantCode) throws Exception {
        String request = """
                {
                  "code": "%s",
                  "name": "Async Knowledge Tenant"
                }
                """.formatted(tenantCode);

        mockMvc.perform(post("/api/v1/bootstrap/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());
    }

    private String registerAndLogin(String tenantCode, String username) throws Exception {
        String registerRequest = """
                {
                  "tenantCode": "%s",
                  "username": "%s",
                  "displayName": "Async Admin",
                  "password": "Password123!"
                }
                """.formatted(tenantCode, username);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest))
                .andExpect(status().isCreated());

        String loginRequest = """
                {
                  "tenantCode": "%s",
                  "username": "%s",
                  "password": "Password123!"
                }
                """.formatted(tenantCode, username);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
    }
}