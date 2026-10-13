package com.encipherhealth.chatapp.repository;

import com.encipherhealth.chatapp.dto.ChatMessageDto;
import com.encipherhealth.chatapp.dto.ChatRoomDetailsResponse;
import com.encipherhealth.chatapp.util.RedisKeyConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class ChatRoomRepository
{
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public ChatRoomRepository(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper)
    {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean createRoom(String roomId)
    {
        String roomKey = RedisKeyConstants.roomKey(roomId);
        Boolean created = stringRedisTemplate.opsForHash()
                .putIfAbsent(roomKey, RedisKeyConstants.HASH_FIELD_ROOM_NAME, roomId);
        if (!Boolean.TRUE.equals(created)) {
            return false;
        }
        stringRedisTemplate.opsForHash().put(roomKey, RedisKeyConstants.HASH_FIELD_CREATED_AT, utcNow());
        stringRedisTemplate.opsForSet().add(RedisKeyConstants.ROOM_INDEX_KEY, roomId);
        return true;
    }

    public boolean exists(String roomId)
    {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeyConstants.roomKey(roomId)));
    }

    public void addParticipant(String roomId, String participant)
    {
        stringRedisTemplate.opsForSet().add(RedisKeyConstants.participantsKey(roomId), participant);
    }

    public boolean isParticipant(String roomId, String participant)
    {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet()
                .isMember(RedisKeyConstants.participantsKey(roomId), participant));
    }

    public Set<String> getParticipants(String roomId)
    {
        Set<String> participants = stringRedisTemplate.opsForSet().members(RedisKeyConstants.participantsKey(roomId));
        return participants == null ? Collections.emptySet() : participants;
    }

    public void appendMessage(String roomId, ChatMessageDto message)
    {
        try {
            stringRedisTemplate.opsForList().rightPush(RedisKeyConstants.messagesKey(roomId),
                    objectMapper.writeValueAsString(message));
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize chat message", e);
        }
    }

    public List<ChatMessageDto> getLastMessages(String roomId, int limit)
    {
        List<String> rawMessages = stringRedisTemplate.opsForList()
                .range(RedisKeyConstants.messagesKey(roomId), -limit, -1);
        if (rawMessages == null || rawMessages.isEmpty()) {
            return new ArrayList<>();
        }
        List<ChatMessageDto> messages = new ArrayList<>(rawMessages.size());
        for (String raw : rawMessages) {
            messages.add(readMessage(raw));
        }
        return messages;
    }

    public ChatRoomDetailsResponse getRoomDetails(String roomId)
    {
        Map<Object, Object> metadata = stringRedisTemplate.opsForHash().entries(RedisKeyConstants.roomKey(roomId));
        String roomName = asString(metadata.get(RedisKeyConstants.HASH_FIELD_ROOM_NAME));
        String createdAt = asString(metadata.get(RedisKeyConstants.HASH_FIELD_CREATED_AT));
        return new ChatRoomDetailsResponse(roomId, roomName, createdAt, new HashSet<>(getParticipants(roomId)));
    }

    public Set<String> listRoomIds()
    {
        Set<String> rooms = stringRedisTemplate.opsForSet().members(RedisKeyConstants.ROOM_INDEX_KEY);
        return rooms == null ? Collections.emptySet() : rooms;
    }

    public void deleteRoom(String roomId)
    {
        stringRedisTemplate.delete(List.of(
                RedisKeyConstants.roomKey(roomId),
                RedisKeyConstants.participantsKey(roomId),
                RedisKeyConstants.messagesKey(roomId)
        ));
        stringRedisTemplate.opsForSet().remove(RedisKeyConstants.ROOM_INDEX_KEY, roomId);
    }

    public void publish(String roomId, ChatMessageDto message)
    {
        try {
            stringRedisTemplate.convertAndSend(RedisKeyConstants.liveChatChannel(roomId),
                    objectMapper.writeValueAsString(message));
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to publish chat message", e);
        }
    }

    public static String utcNow()
    {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }

    private ChatMessageDto readMessage(String raw)
    {
        try {
            return objectMapper.readValue(raw, ChatMessageDto.class);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to deserialize chat message", e);
        }
    }

    private String asString(Object value)
    {
        return value == null ? null : String.valueOf(value);
    }
}
