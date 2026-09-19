package com.smartdesk;

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

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:observability;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgentObservabilityApiTests {

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
        when(conversationMemoryService.getRecent(anyLong(), anyInt())).thenReturn(List.of());
    }

    @Test
    void shouldExposeAgentRunsAndTenantMetrics() throws Exception {
        bootstrapTenant();
        String adminToken = registerAndLogin("admin");
        long conversationId = createConversation(adminToken);
        chat(adminToken, conversationId, "订单号 A10001 什么时候到？");

        MvcResult listResult = mockMvc.perform(get(
                        "/api/v1/conversations/{id}/agent-runs", conversationId
                )
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].route").value("TOOL_CALL"))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.data[0].durationMs").isNumber())
                .andExpect(jsonPath("$.data[0].toolCalls.length()").value(1))
                .andExpect(jsonPath("$.data[0].toolCalls[0].toolName").value("queryOrder"))
                .andExpect(jsonPath("$.data[0].toolCalls[0].arguments.orderNo").value("A10001"))
                .andExpect(jsonPath("$.data[0].toolCalls[0].result.status").value("SHIPPED"))
                .andReturn();

        long runId = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .path("data").path(0).path("id").asLong();
        mockMvc.perform(get(
                        "/api/v1/conversations/{conversationId}/agent-runs/{runId}",
                        conversationId, runId
                )
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(runId));

        mockMvc.perform(get("/api/v1/agent/metrics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRuns").value(1))
                .andExpect(jsonPath("$.data.completedRuns").value(1))
                .andExpect(jsonPath("$.data.totalToolCalls").value(1))
                .andExpect(jsonPath("$.data.successfulToolCalls").value(1))
                .andExpect(jsonPath("$.data.toolSuccessRate").value(1.0));

        String memberToken = registerAndLogin("member");
        mockMvc.perform(get("/api/v1/agent/metrics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/conversations/{id}/agent-runs", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isNotFound());
    }

    private void bootstrapTenant() throws Exception {
        mockMvc.perform(post("/api/v1/bootstrap/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"observabilityco\",\"name\":\"Observability Tenant\"}"))
                .andExpect(status().isCreated());
    }

    private String registerAndLogin(String username) throws Exception {
        String registerRequest = """
                {
                  "tenantCode":"observabilityco",
                  "username":"%s",
                  "displayName":"Observability User",
                  "password":"Password123!"
                }
                """.formatted(username);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest))
                .andExpect(status().isCreated());

        String loginRequest = """
                {"tenantCode":"observabilityco","username":"%s","password":"Password123!"}
                """.formatted(username);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private long createConversation(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/conversations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Observability chat\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private void chat(String token, long conversationId, String message) throws Exception {
        MvcResult asyncResult = mockMvc.perform(post(
                        "/api/v1/conversations/{id}/chat", conversationId
                )
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"message\":\"" + message + "\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:done")));
    }
}
