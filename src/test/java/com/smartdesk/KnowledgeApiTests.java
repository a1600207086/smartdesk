package com.smartdesk;

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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:knowledge;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class KnowledgeApiTests {

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
        when(knowledgeSearchCache.currentVersion(anyLong())).thenReturn("test");
        when(knowledgeSearchCache.get(anyLong(), anyString(), anyInt(), anyString()))
                .thenReturn(List.of());
    }

    @Test
    void shouldUploadTextAndRetrieveRelevantChunk() throws Exception {
        ensureTenant("knowledgeco");
        String token = registerAndLogin("knowledgeco", "knowledgeadmin");

        String document = """
                {
                  "title": "退款政策",
                  "content": "退款政策：商品签收后七天内可以申请无理由退款。退款将在审核通过后三个工作日到账。",
                  "sourceUri": "internal://policy/refund"
                }
                """;

        mockMvc.perform(post("/api/v1/knowledge/documents/text")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(document))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.chunkCount").value(1));

        String search = """
                {
                  "query": "退货后多久可以收到退款？",
                  "topK": 3
                }
                """;

        mockMvc.perform(post("/api/v1/knowledge/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(search))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].documentTitle").value("退款政策"))
                .andExpect(jsonPath("$.data[0].content").value(org.hamcrest.Matchers.containsString("三个工作日")))
                .andExpect(jsonPath("$.data[0].score").isNumber());
    }

    @Test
    void shouldReindexAndDeleteDocument() throws Exception {
        ensureTenant("knowledgeco");
        String token = registerAndLogin("knowledgeco", "knowledgeadmin");

        String document = """
                {
                  "title": "物流规则",
                  "content": "物流规则：普通快递预计三个工作日送达，偏远地区可能需要五个工作日。",
                  "sourceUri": "internal://policy/logistics"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/knowledge/documents/text")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(document))
                .andExpect(status().isCreated())
                .andReturn();

        long documentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        mockMvc.perform(post("/api/v1/knowledge/documents/{id}/reindex", documentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.embeddingModel").value("hash-v1-256"))
                .andExpect(jsonPath("$.data.chunkCount").value(1));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/v1/knowledge/documents/{id}",
                        documentId
                )
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        String search = """
                {
                  "query": "普通快递多久送达？",
                  "topK": 3
                }
                """;

        mockMvc.perform(post("/api/v1/knowledge/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(search))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private void ensureTenant(String tenantCode) throws Exception {
        String request = """
                {
                  "code": "%s",
                  "name": "Knowledge Tenant"
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
                  "displayName": "Knowledge Admin",
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
