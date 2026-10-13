package com.encipherhealth.chatapp;

import com.encipherhealth.chatapp.pubsub.ChatMessageSubscriber;
import com.encipherhealth.chatapp.support.RedisTestSupport;
import com.encipherhealth.chatapp.util.RedisKeyConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Set;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatApplicationIntegrationTest
{
    private static final RedisTestSupport REDIS = new RedisTestSupport();

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry)
    {
        REDIS.start();
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", REDIS::port);
        registry.add("spring.data.redis.database", () -> 0);
    }

    @AfterAll
    static void stopRedis()
    {
        REDIS.stop();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ChatMessageSubscriber chatMessageSubscriber;

    @BeforeEach
    void cleanRedis()
    {
        Set<String> keys = stringRedisTemplate.keys("chat:*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
        chatMessageSubscriber.clear("general");
        chatMessageSubscriber.clear("missing");
    }

    @Test
    void createAndJoinStoresRoomHashAndParticipantSet()
            throws Exception
    {
        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomName":"general"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.roomId").value("general"));

        mockMvc.perform(post("/api/chatapp/chatrooms/general/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participant":"guest_user"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        Map<Object, Object> metadata = stringRedisTemplate.opsForHash().entries(RedisKeyConstants.roomKey("general"));
        assertEquals("general", metadata.get(RedisKeyConstants.HASH_FIELD_ROOM_NAME));
        assertTrue(metadata.containsKey(RedisKeyConstants.HASH_FIELD_CREATED_AT));
        assertTrue(stringRedisTemplate.opsForSet().isMember(RedisKeyConstants.participantsKey("general"), "guest_user"));
    }

    @Test
    void sendAndRetrieveMessagesKeepsChronologicalOrder()
            throws Exception
    {
        createAndJoin("general", "guest_user");

        mockMvc.perform(post("/api/chatapp/chatrooms/general/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participant":"guest_user","message":"Hello, everyone!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        mockMvc.perform(post("/api/chatapp/chatrooms/general/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participant":"another_user"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/chatapp/chatrooms/general/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participant":"another_user","message":"Hi, guest_user!"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/chatapp/chatrooms/general/messages").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].participant").value("another_user"))
                .andExpect(jsonPath("$.messages[0].message").value("Hi, guest_user!"));

        mockMvc.perform(get("/api/chatapp/chatrooms/general/messages").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[0].message").value("Hello, everyone!"))
                .andExpect(jsonPath("$.messages[1].message").value("Hi, guest_user!"));
    }

    @Test
    void publishedMessageIsReceivedByRoomSubscribers()
            throws Exception
    {
        createAndJoin("general", "guest_user");

        mockMvc.perform(post("/api/chatapp/chatrooms/general/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participant":"guest_user","message":"Hello, everyone!"}
                                """))
                .andExpect(status().isOk());

        await().untilAsserted(() -> {
            assertFalse(chatMessageSubscriber.receivedFor("general").isEmpty());
            assertTrue(chatMessageSubscriber.receivedFor("general").get(0).contains("Hello, everyone!"));
        });
    }

    @Test
    void sendMessageToUnknownRoomReturnsError()
            throws Exception
    {
        mockMvc.perform(post("/api/chatapp/chatrooms/missing/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participant":"guest_user","message":"Hello, everyone!"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Chat room 'missing' does not exist."));
    }

    @Test
    void deleteChatRoomRemovesAllRedisKeys()
            throws Exception
    {
        createAndJoin("general", "guest_user");
        mockMvc.perform(post("/api/chatapp/chatrooms/general/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participant":"guest_user","message":"Hello, everyone!"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/chatapp/chatrooms/general"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        assertFalse(Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeyConstants.roomKey("general"))));
        assertFalse(Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeyConstants.participantsKey("general"))));
        assertFalse(Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeyConstants.messagesKey("general"))));
        assertFalse(Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(RedisKeyConstants.ROOM_INDEX_KEY, "general")));
    }

    private void createAndJoin(String roomId, String participant)
            throws Exception
    {
        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomName\":\"" + roomId + "\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/chatapp/chatrooms/" + roomId + "/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participant\":\"" + participant + "\"}"))
                .andExpect(status().isOk());
    }
}
