package com.encipherhealth.chatapp.pubsub;

import com.encipherhealth.chatapp.util.RedisKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class ChatMessageSubscriber implements MessageListener
{
    private final Map<String, CopyOnWriteArrayList<String>> receivedMessages = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emittersByRoom = new ConcurrentHashMap<>();

    @Override
    public void onMessage(Message message, byte[] pattern)
    {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        String roomId = RedisKeyConstants.roomIdFromChannel(channel);
        receivedMessages.computeIfAbsent(roomId, key -> new CopyOnWriteArrayList<>()).add(payload);
        log.info("Received live chat message | roomId={} | payload={}", roomId, payload);
        fanOut(roomId, payload);
    }

    public void register(String roomId, SseEmitter emitter)
    {
        emittersByRoom.computeIfAbsent(roomId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable cleanup = () -> removeEmitter(roomId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());
    }

    public List<String> receivedFor(String roomId)
    {
        return new ArrayList<>(receivedMessages.getOrDefault(roomId, new CopyOnWriteArrayList<>()));
    }

    public void clear(String roomId)
    {
        receivedMessages.remove(roomId);
        emittersByRoom.remove(roomId);
    }

    private void fanOut(String roomId, String payload)
    {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByRoom.get(roomId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("message").data(payload));
            }
            catch (IOException e) {
                removeEmitter(roomId, emitter);
            }
        }
    }

    private void removeEmitter(String roomId, SseEmitter emitter)
    {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByRoom.get(roomId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }
}
