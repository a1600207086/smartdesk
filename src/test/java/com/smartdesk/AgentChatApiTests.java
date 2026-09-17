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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
        "spring.datasource.url=jdbc:h2:mem:agentchat;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgentChatApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void shouldStreamDirectAgentResponseAndPersistMessages() throws Exception {
        ensureTenant("agentco");
        String token = registerAndLogin("agentco", "agentuser1");
        long conversationId = createConversation(token, "Direct chat");

        String stream = chat(token, conversationId, "你好，请介绍一下你能做什么");

        assertThat(stream).contains("event:route");
        assertThat(stream).contains("event:message");
        assertThat(stream).contains("Mock Agent");
        assertThat(stream).contains("event:done");

        mockMvc.perform(get("/api/v1/conversations/{id}/messages", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].role").value("USER"))
                .andExpect(jsonPath("$.data.items[1].role").value("ASSISTANT"));
    }

    @Test
    void shouldExecuteOrderToolAndRecordLogs() throws Exception {
        ensureTenant("agentco");
        String token = registerAndLogin("agentco", "agentuser2");
        long conversationId = createConversation(token, "Order chat");

        String stream = chat(token, conversationId, "订单号 A10001 什么时候到？");

        assertThat(stream).contains("event:route");
        assertThat(stream).contains("event:tool");
        assertThat(stream).contains("\"toolName\":\"queryOrder\"");
        assertThat(stream).contains("\"status\":\"COMPLETED\"");
        assertThat(stream).contains("SHIPPED");
        assertThat(stream).contains("event:done");

        Integer runCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agent_run WHERE route = 'TOOL_CALL'",
                Integer.class
        );
        Integer toolCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tool_call_log WHERE success = TRUE",
                Integer.class
        );
        assertThat(runCount).isNotNull().isGreaterThan(0);
        assertThat(toolCount).isNotNull().isGreaterThan(0);
    }

    private void ensureTenant(String tenantCode) throws Exception {
        String request = """
                {
                  "code": "%s",
                  "name": "Agent Tenant"
                }
                """.formatted(tenantCode);

        MvcResult result = mockMvc.perform(post("/api/v1/bootstrap/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isIn(201, 409);
    }

    private String registerAndLogin(String tenantCode, String username) throws Exception {
        String registerRequest = """
                {
                  "tenantCode": "%s",
                  "username": "%s",
                  "displayName": "Agent User",
                  "password": "Password123!"
                }
                """.formatted(tenantCode, username);

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest))
                .andReturn();
        assertThat(registerResult.getResponse().getStatus()).isIn(201, 409);

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

    private String chat(String token, long conversationId, String message) throws Exception {
        String request = """
                {
                  "message": "%s"
                }
                """.formatted(message);

        MvcResult asyncResult = mockMvc.perform(post(
                        "/api/v1/conversations/{id}/chat",
                        conversationId
                )
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(request))
                .andExpect(request().asyncStarted())
                .andReturn();

        return mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:done")))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
