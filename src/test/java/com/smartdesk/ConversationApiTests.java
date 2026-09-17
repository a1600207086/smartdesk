package com.smartdesk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdesk.auth.LoginAttemptGuard;
import com.smartdesk.auth.TokenBlacklistService;
import com.smartdesk.conversation.ConversationMemoryService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ConversationApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoginAttemptGuard loginAttemptGuard;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private ConversationMemoryService conversationMemoryService;

    @BeforeEach
    void setUpConversationMemory() {
        when(conversationMemoryService.getRecent(anyLong(), anyInt()))
                .thenReturn(List.of());
    }

    @Test
    void shouldCreateConversationAndPageMessages() throws Exception {
        String token = bootstrapAndLogin("chatco", "alice");
        long conversationId = createConversation(token, "订单咨询");

        long firstId = appendMessage(token, conversationId, "message-1");
        long secondId = appendMessage(token, conversationId, "message-2");
        long thirdId = appendMessage(token, conversationId, "message-3");

        MvcResult firstPage = mockMvc.perform(get(
                        "/api/v1/conversations/{id}/messages",
                        conversationId
                )
                        .param("limit", "2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(secondId))
                .andExpect(jsonPath("$.data.items[0].content").value("message-2"))
                .andExpect(jsonPath("$.data.items[1].id").value(thirdId))
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andReturn();

        long nextBeforeId = objectMapper.readTree(firstPage.getResponse().getContentAsString())
                .path("data")
                .path("nextBeforeId")
                .asLong();

        mockMvc.perform(get(
                        "/api/v1/conversations/{id}/messages",
                        conversationId
                )
                        .param("limit", "2")
                        .param("beforeId", Long.toString(nextBeforeId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(firstId))
                .andExpect(jsonPath("$.data.items[0].content").value("message-1"))
                .andExpect(jsonPath("$.data.hasMore").value(false));
    }

    @Test
    void shouldPreventCrossTenantConversationAccess() throws Exception {
        String aliceToken = bootstrapAndLogin("alpha", "alice");
        createTenant(aliceToken, "beta", "Beta Company");
        String bobToken = registerAndLogin("beta", "bob");

        long aliceConversationId = createConversation(aliceToken, "Alice private conversation");

        mockMvc.perform(get(
                        "/api/v1/conversations/{id}",
                        aliceConversationId
                )
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bobToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private String bootstrapAndLogin(String tenantCode, String username) throws Exception {
        String bootstrapRequest = """
                {
                  "code": "%s",
                  "name": "Conversation Tenant"
                }
                """.formatted(tenantCode);

        mockMvc.perform(post("/api/v1/bootstrap/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bootstrapRequest))
                .andExpect(status().isCreated());

        return registerAndLogin(tenantCode, username);
    }

    private String registerAndLogin(String tenantCode, String username) throws Exception {
        String registerRequest = """
                {
                  "tenantCode": "%s",
                  "username": "%s",
                  "displayName": "Conversation User",
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

    private void createTenant(String token, String code, String name) throws Exception {
        String request = """
                {
                  "code": "%s",
                  "name": "%s"
                }
                """.formatted(code, name);

        mockMvc.perform(post("/api/v1/tenants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());
    }

    private long createConversation(String token, String title) throws Exception {
        String request = """
                {
                  "title": "%s"
                }
                """.formatted(title);

        MvcResult result = mockMvc.perform(post("/api/v1/conversations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();
    }

    private long appendMessage(String token, long conversationId, String content) throws Exception {
        String request = """
                {
                  "content": "%s"
                }
                """.formatted(content);

        MvcResult result = mockMvc.perform(post(
                        "/api/v1/conversations/{id}/messages",
                        conversationId
                )
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("id").asLong();
    }
}