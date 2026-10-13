package com.encipherhealth.chatapp.service;

import com.encipherhealth.chatapp.dto.ChatHistoryResponse;
import com.encipherhealth.chatapp.dto.ChatMessageDto;
import com.encipherhealth.chatapp.dto.ChatRoomDetailsResponse;
import com.encipherhealth.chatapp.dto.StatusResponse;
import com.encipherhealth.chatapp.exception.ChatRoomNotFoundException;
import com.encipherhealth.chatapp.exception.CustomException;
import com.encipherhealth.chatapp.exception.DuplicateChatRoomException;
import com.encipherhealth.chatapp.exception.ParticipantNotJoinedException;
import com.encipherhealth.chatapp.repository.ChatRoomRepository;
import com.encipherhealth.chatapp.util.Messages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
public class ChatRoomServiceImpl implements ChatRoomService
{
    private static final int DEFAULT_LIMIT = 10;

    private final ChatRoomRepository chatRoomRepository;

    public ChatRoomServiceImpl(ChatRoomRepository chatRoomRepository)
    {
        this.chatRoomRepository = chatRoomRepository;
    }

    @Override
    public StatusResponse createChatRoom(String roomName)
    {
        long startTime = System.currentTimeMillis();
        String roomId = normalize(roomName);
        log.info("Entering method createChatRoom | params={roomName={}}", roomId);
        if (!chatRoomRepository.createRoom(roomId)) {
            throw new DuplicateChatRoomException(Messages.chatRoomAlreadyExists(roomId));
        }
        StatusResponse response = StatusResponse.success(Messages.chatRoomCreated(roomId), roomId);
        log.info("Exiting method createChatRoom | outcome=success | duration={}ms",
                System.currentTimeMillis() - startTime);
        return response;
    }

    @Override
    public StatusResponse joinChatRoom(String roomId, String participant)
    {
        long startTime = System.currentTimeMillis();
        String normalizedRoomId = normalize(roomId);
        String normalizedParticipant = normalize(participant);
        log.info("Entering method joinChatRoom | params={roomId={}, participant={}}",
                normalizedRoomId, normalizedParticipant);
        ensureRoomExists(normalizedRoomId);
        chatRoomRepository.addParticipant(normalizedRoomId, normalizedParticipant);
        StatusResponse response = StatusResponse.success(Messages.userJoined(normalizedParticipant, normalizedRoomId));
        log.info("Exiting method joinChatRoom | outcome=success | duration={}ms",
                System.currentTimeMillis() - startTime);
        return response;
    }

    @Override
    public StatusResponse sendMessage(String roomId, String participant, String message)
    {
        long startTime = System.currentTimeMillis();
        String normalizedRoomId = normalize(roomId);
        String normalizedParticipant = normalize(participant);
        log.info("Entering method sendMessage | params={roomId={}, participant={}}",
                normalizedRoomId, normalizedParticipant);
        ensureRoomExists(normalizedRoomId);
        if (!chatRoomRepository.isParticipant(normalizedRoomId, normalizedParticipant)) {
            throw new ParticipantNotJoinedException(Messages.userNotJoined(normalizedParticipant, normalizedRoomId));
        }
        ChatMessageDto chatMessage = new ChatMessageDto(normalizedParticipant, message.trim(), ChatRoomRepository.utcNow());
        chatRoomRepository.appendMessage(normalizedRoomId, chatMessage);
        chatRoomRepository.publish(normalizedRoomId, chatMessage);
        StatusResponse response = StatusResponse.success(Messages.messageSent());
        log.info("Exiting method sendMessage | outcome=success | duration={}ms",
                System.currentTimeMillis() - startTime);
        return response;
    }

    @Override
    public ChatHistoryResponse getMessages(String roomId, Integer limit)
    {
        long startTime = System.currentTimeMillis();
        String normalizedRoomId = normalize(roomId);
        int resolvedLimit = resolveLimit(limit);
        log.info("Entering method getMessages | params={roomId={}, limit={}}", normalizedRoomId, resolvedLimit);
        ensureRoomExists(normalizedRoomId);
        ChatHistoryResponse response = new ChatHistoryResponse(
                chatRoomRepository.getLastMessages(normalizedRoomId, resolvedLimit));
        log.info("Exiting method getMessages | outcome=success | count={} | duration={}ms",
                response.getMessages().size(), System.currentTimeMillis() - startTime);
        return response;
    }

    @Override
    public StatusResponse deleteChatRoom(String roomId)
    {
        long startTime = System.currentTimeMillis();
        String normalizedRoomId = normalize(roomId);
        log.info("Entering method deleteChatRoom | params={roomId={}}", normalizedRoomId);
        ensureRoomExists(normalizedRoomId);
        chatRoomRepository.deleteRoom(normalizedRoomId);
        StatusResponse response = StatusResponse.success(Messages.chatRoomDeleted(normalizedRoomId), normalizedRoomId);
        log.info("Exiting method deleteChatRoom | outcome=success | duration={}ms",
                System.currentTimeMillis() - startTime);
        return response;
    }

    @Override
    public ChatRoomDetailsResponse getChatRoom(String roomId)
    {
        String normalizedRoomId = normalize(roomId);
        log.info("Entering method getChatRoom | params={roomId={}}", normalizedRoomId);
        ensureRoomExists(normalizedRoomId);
        return chatRoomRepository.getRoomDetails(normalizedRoomId);
    }

    @Override
    public Set<String> listChatRooms()
    {
        log.info("Entering method listChatRooms");
        return chatRoomRepository.listRoomIds();
    }

    private void ensureRoomExists(String roomId)
    {
        if (!chatRoomRepository.exists(roomId)) {
            throw new ChatRoomNotFoundException(Messages.chatRoomDoesNotExist(roomId));
        }
    }

    private int resolveLimit(Integer limit)
    {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit <= 0) {
            throw new CustomException(Messages.invalidLimit());
        }
        return limit;
    }

    private String normalize(String value)
    {
        return value == null ? "" : value.trim();
    }
}
