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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:feedback;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MessageFeedbackApiTests {

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
        when(conversationMemoryService.getRecent(anyLong(), anyInt())).thenReturn(List.of());
    }

    @Test
    void shouldManageFeedbackAndTenantSummary() throws Exception {
        bootstrapTenant("feedbackco");
        String adminToken = registerAndLogin("feedbackco", "admin");
        long conversationId = createConversation(adminToken);
        chat(adminToken, conversationId, "Hello SmartDesk");

        JsonNode messages = findMessages(adminToken, conversationId);
        long userMessageId = findMessageId(messages, "USER");
        long assistantMessageId = findMessageId(messages, "ASSISTANT");

        MvcResult firstFeedback = putFeedback(
                adminToken,
                conversationId,
                assistantMessageId,
                "HELPFUL",
                "回答准确"
        );
        long feedbackId = objectMapper.readTree(firstFeedback.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        MvcResult updatedFeedback = putFeedback(
                adminToken,
                conversationId,
                assistantMessageId,
                "NOT_HELPFUL",
                "还需要更具体"
        );
        long updatedFeedbackId = objectMapper.readTree(updatedFeedback.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();
        assertThat(updatedFeedbackId).isEqualTo(feedbackId);

        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM message_feedback WHERE message_id = ?",
                Integer.class,
                assistantMessageId
        );
        assertThat(rowCount).isEqualTo(1);

        mockMvc.perform(get(feedbackUri(conversationId, assistantMessageId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value("NOT_HELPFUL"))
                .andExpect(jsonPath("$.data.comment").value("还需要更具体"));

        mockMvc.perform(get("/api/v1/feedback/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.helpfulCount").value(0))
                .andExpect(jsonPath("$.data.notHelpfulCount").value(1))
                .andExpect(jsonPath("$.data.helpfulRate").value(0.0));

        mockMvc.perform(put(feedbackUri(conversationId, userMessageId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":\"HELPFUL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        String memberToken = registerAndLogin("feedbackco", "member");
        mockMvc.perform(get("/api/v1/feedback/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(feedbackUri(conversationId, assistantMessageId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":\"HELPFUL\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete(feedbackUri(conversationId, assistantMessageId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get(feedbackUri(conversationId, assistantMessageId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    private MvcResult putFeedback(
            String token,
            long conversationId,
            long messageId,
            String rating,
            String comment
    ) throws Exception {
        String request = """
                {"rating":"%s","comment":"%s"}
                """.formatted(rating, comment);
        return mockMvc.perform(put(feedbackUri(conversationId, messageId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(rating))
                .andReturn();
    }

    private String feedbackUri(long conversationId, long messageId) {
        return "/api/v1/conversations/" + conversationId
                + "/messages/" + messageId + "/feedback";
    }

    private void bootstrapTenant(String code) throws Exception {
        mockMvc.perform(post("/api/v1/bootstrap/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\",\"name\":\"Feedback Tenant\"}"))
                .andExpect(status().isCreated());
    }

    private String registerAndLogin(String tenantCode, String username) throws Exception {
        String registerRequest = """
                {
                  "tenantCode":"%s",
                  "username":"%s",
                  "displayName":"Feedback User",
                  "password":"Password123!"
                }
                """.formatted(tenantCode, username);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest))
                .andExpect(status().isCreated());

        String loginRequest = """
                {"tenantCode":"%s","username":"%s","password":"Password123!"}
                """.formatted(tenantCode, username);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
    }

    private long createConversation(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/conversations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Feedback chat\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();
    }

    private void chat(String token, long conversationId, String message) throws Exception {
        MvcResult asyncResult = mockMvc.perform(post(
                        "/api/v1/conversations/{id}/chat",
                        conversationId
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

    private JsonNode findMessages(String token, long conversationId) throws Exception {
        MvcResult result = mockMvc.perform(get(
                        "/api/v1/conversations/{id}/messages",
                        conversationId
                )
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("items");
    }

    private long findMessageId(JsonNode messages, String role) {
        for (JsonNode message : messages) {
            if (role.equals(message.path("role").asText())) {
                return message.path("id").asLong();
            }
        }
        throw new AssertionError("Missing message role: " + role);
    }
}
