package com.smartdesk.conversation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConversationMemoryService {

    private static final Logger log = LoggerFactory.getLogger(ConversationMemoryService.class);
    private static final String KEY_PREFIX = "smartdesk:conversation:recent:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ConversationProperties properties;

    public ConversationMemoryService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            ConversationProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public List<MessageResponse> getRecent(Long conversationId, int limit) {
        try {
            List<String> values = redisTemplate.opsForList()
                    .range(redisKey(conversationId), -limit, -1);
            if (values == null || values.isEmpty()) {
                return List.of();
            }

            List<MessageResponse> messages = new ArrayList<>(values.size());
            for (String value : values) {
                messages.add(objectMapper.readValue(value, MessageResponse.class));
            }
            return List.copyOf(messages);
        } catch (RuntimeException | JsonProcessingException exception) {
            log.warn("Unable to read conversation memory from Redis", exception);
            return List.of();
        }
    }

    public void append(Long conversationId, MessageResponse message) {
        try {
            String key = redisKey(conversationId);
            redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(message));
            redisTemplate.opsForList().trim(key, -properties.recentMemorySize(), -1);
            redisTemplate.expire(key, properties.memoryTtl());
        } catch (RuntimeException | JsonProcessingException exception) {
            log.warn("Unable to append conversation memory to Redis", exception);
        }
    }

    public void replaceRecent(Long conversationId, List<MessageResponse> messages) {
        try {
            String key = redisKey(conversationId);
            redisTemplate.delete(key);
            if (messages.isEmpty()) {
                return;
            }

            List<String> values = new ArrayList<>(messages.size());
            for (MessageResponse message : messages) {
                values.add(objectMapper.writeValueAsString(message));
            }
            redisTemplate.opsForList().rightPushAll(key, values);
            redisTemplate.opsForList().trim(key, -properties.recentMemorySize(), -1);
            redisTemplate.expire(key, properties.memoryTtl());
        } catch (RuntimeException | JsonProcessingException exception) {
            log.warn("Unable to replace conversation memory in Redis", exception);
        }
    }

    private String redisKey(Long conversationId) {
        return KEY_PREFIX + conversationId;
    }
}