package com.smartdesk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdesk.auth.LoginAttemptGuard;
import com.smartdesk.auth.TokenBlacklistService;
import com.smartdesk.conversation.ConversationMemoryService;
import com.smartdesk.knowledge.KnowledgeRetrievalService;
import com.smartdesk.knowledge.KnowledgeSearchResult;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
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

    @MockBean
    private KnowledgeRetrievalService knowledgeRetrievalService;

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

    @Test
    void shouldStreamConciseKnowledgeAnswerWithCitations() throws Exception {
        ensureTenant("agentco");
        String token = registerAndLogin("agentco", "knowledgeuser");
        long conversationId = createConversation(token, "Knowledge chat");
        String question = "银行卡退款需要几个工作日到账？";
        when(knowledgeRetrievalService.search(anyLong(), anyString(), nullable(Integer.class)))
                .thenReturn(List.of(new KnowledgeSearchResult(
                        5L,
                        "Refund Policy Demo",
                        6L,
                        0,
                        "支付宝或微信到账：商家发起退款后 1 至 3 个工作日。\n"
                                + "银行卡到账：商家发起退款后 3 至 7 个工作日，具体以发卡银行为准。",
                        0.5583
                )));

        String stream = chat(token, conversationId, question);

        assertThat(stream).contains("event:citation");
        assertThat(stream).contains("\"documentId\":5");
        assertThat(stream).contains("\"documentTitle\":\"Refund Policy Demo\"");
        assertThat(stream).contains("\"matchCount\":1");
        assertThat(stream).contains("银行卡到账：商家发起退款后 3 至 7 个工作日");
        assertThat(stream).contains("来源：[1] Refund Policy Demo");
        assertThat(stream).doesNotContain("KnowledgeSearchResult[");

        Integer citationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM message_citation WHERE document_id = 5",
                Integer.class
        );
        assertThat(citationCount).isEqualTo(1);

        mockMvc.perform(get("/api/v1/conversations/{id}/messages", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.items[1].citations.length()").value(1))
                .andExpect(jsonPath("$.data.items[1].citations[0].index").value(1))
                .andExpect(jsonPath("$.data.items[1].citations[0].documentId").value(5))
                .andExpect(jsonPath("$.data.items[1].citations[0].documentTitle")
                        .value("Refund Policy Demo"))
                .andExpect(jsonPath("$.data.items[1].citations[0].chunkId").value(6));
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
