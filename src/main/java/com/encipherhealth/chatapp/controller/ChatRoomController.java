package com.encipherhealth.chatapp.controller;

import com.encipherhealth.chatapp.config.StatusCode;
import com.encipherhealth.chatapp.dto.ChatHistoryResponse;
import com.encipherhealth.chatapp.dto.ChatRoomDetailsResponse;
import com.encipherhealth.chatapp.dto.CreateChatRoomRequest;
import com.encipherhealth.chatapp.dto.JoinChatRoomRequest;
import com.encipherhealth.chatapp.dto.SendMessageRequest;
import com.encipherhealth.chatapp.dto.StatusResponse;
import com.encipherhealth.chatapp.exception.ChatRoomNotFoundException;
import com.encipherhealth.chatapp.exception.CustomException;
import com.encipherhealth.chatapp.exception.DuplicateChatRoomException;
import com.encipherhealth.chatapp.exception.ParticipantNotJoinedException;
import com.encipherhealth.chatapp.pubsub.ChatMessageSubscriber;
import com.encipherhealth.chatapp.service.ChatRoomService;
import com.encipherhealth.chatapp.util.Messages;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/chatapp/chatrooms")
@Tag(name = "Chat Rooms")
public class ChatRoomController
{
    private static final long SSE_TIMEOUT_MS = TimeUnit.HOURS.toMillis(1);

    private final ChatRoomService chatRoomService;
    private final ChatMessageSubscriber chatMessageSubscriber;

    public ChatRoomController(ChatRoomService chatRoomService, ChatMessageSubscriber chatMessageSubscriber)
    {
        this.chatRoomService = chatRoomService;
        this.chatMessageSubscriber = chatMessageSubscriber;
    }

    @PostMapping
    @Operation(summary = "Create a chat room")
    public ResponseEntity<StatusResponse> createChatRoom(@Valid @RequestBody CreateChatRoomRequest request)
    {
        long startTime = System.currentTimeMillis();
        log.info("Entering method createChatRoom | params={roomName={}}", request.getRoomName());
        try {
            StatusResponse response = chatRoomService.createChatRoom(request.getRoomName());
            log.info("Exiting method createChatRoom | outcome=success | duration={}ms",
                    System.currentTimeMillis() - startTime);
            return ResponseEntity.status(StatusCode.CREATED).body(response);
        }
        catch (DuplicateChatRoomException e) {
            log.error("Exception in method createChatRoom | error={}", e.getMessage());
            return ResponseEntity.status(StatusCode.CONFLICT).body(StatusResponse.error(e.getMessage()));
        }
        catch (Exception e) {
            log.error("Exception in method createChatRoom | error={}", e.getMessage(), e);
            return ResponseEntity.status(StatusCode.EXCEPTION).body(StatusResponse.error(Messages.FAILED));
        }
    }

    @PostMapping("/{roomId}/join")
    @Operation(summary = "Join a chat room")
    public ResponseEntity<StatusResponse> joinChatRoom(@PathVariable String roomId,
            @Valid @RequestBody JoinChatRoomRequest request)
    {
        long startTime = System.currentTimeMillis();
        log.info("Entering method joinChatRoom | params={roomId={}, participant={}}", roomId, request.getParticipant());
        try {
            StatusResponse response = chatRoomService.joinChatRoom(roomId, request.getParticipant());
            log.info("Exiting method joinChatRoom | outcome=success | duration={}ms",
                    System.currentTimeMillis() - startTime);
            return ResponseEntity.status(StatusCode.OK).body(response);
        }
        catch (ChatRoomNotFoundException e) {
            log.error("Exception in method joinChatRoom | error={}", e.getMessage());
            return ResponseEntity.status(StatusCode.NOT_FOUND).body(StatusResponse.error(e.getMessage()));
        }
        catch (Exception e) {
            log.error("Exception in method joinChatRoom | error={}", e.getMessage(), e);
            return ResponseEntity.status(StatusCode.EXCEPTION).body(StatusResponse.error(Messages.FAILED));
        }
    }

    @PostMapping("/{roomId}/messages")
    @Operation(summary = "Send a message")
    public ResponseEntity<StatusResponse> sendMessage(@PathVariable String roomId,
            @Valid @RequestBody SendMessageRequest request)
    {
        long startTime = System.currentTimeMillis();
        log.info("Entering method sendMessage | params={roomId={}, participant={}}", roomId, request.getParticipant());
        try {
            StatusResponse response = chatRoomService.sendMessage(roomId, request.getParticipant(), request.getMessage());
            log.info("Exiting method sendMessage | outcome=success | duration={}ms",
                    System.currentTimeMillis() - startTime);
            return ResponseEntity.status(StatusCode.OK).body(response);
        }
        catch (ChatRoomNotFoundException e) {
            log.error("Exception in method sendMessage | error={}", e.getMessage());
            return ResponseEntity.status(StatusCode.NOT_FOUND).body(StatusResponse.error(e.getMessage()));
        }
        catch (ParticipantNotJoinedException e) {
            log.error("Exception in method sendMessage | error={}", e.getMessage());
            return ResponseEntity.status(StatusCode.UNPROCESSABLE_ENTITY).body(StatusResponse.error(e.getMessage()));
        }
        catch (Exception e) {
            log.error("Exception in method sendMessage | error={}", e.getMessage(), e);
            return ResponseEntity.status(StatusCode.EXCEPTION).body(StatusResponse.error(Messages.FAILED));
        }
    }

    @GetMapping("/{roomId}/messages")
    @Operation(summary = "Retrieve chat history")
    public ResponseEntity<?> getMessages(@PathVariable String roomId,
            @RequestParam(name = "limit", required = false, defaultValue = "10") Integer limit)
    {
        long startTime = System.currentTimeMillis();
        log.info("Entering method getMessages | params={roomId={}, limit={}}", roomId, limit);
        try {
            ChatHistoryResponse response = chatRoomService.getMessages(roomId, limit);
            log.info("Exiting method getMessages | outcome=success | duration={}ms",
                    System.currentTimeMillis() - startTime);
            return ResponseEntity.status(StatusCode.OK).body(response);
        }
        catch (ChatRoomNotFoundException e) {
            log.error("Exception in method getMessages | error={}", e.getMessage());
            return ResponseEntity.status(StatusCode.NOT_FOUND).body(StatusResponse.error(e.getMessage()));
        }
        catch (CustomException e) {
            log.error("Exception in method getMessages | error={}", e.getMessage());
            return ResponseEntity.status(StatusCode.UNPROCESSABLE_ENTITY).body(StatusResponse.error(e.getMessage()));
        }
        catch (Exception e) {
            log.error("Exception in method getMessages | error={}", e.getMessage(), e);
            return ResponseEntity.status(StatusCode.EXCEPTION).body(StatusResponse.error(Messages.FAILED));
        }
    }

    @GetMapping("/{roomId}/stream")
    @Operation(summary = "Subscribe to real-time messages")
    public SseEmitter streamMessages(@PathVariable String roomId)
    {
        log.info("Entering method streamMessages | params={roomId={}}", roomId);
        chatRoomService.getChatRoom(roomId);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        chatMessageSubscriber.register(roomId.trim(), emitter);
        try {
            emitter.send(SseEmitter.event().name("connected").data("subscribed to " + roomId));
        }
        catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    @GetMapping("/{roomId}")
    @Operation(summary = "Get chat room details")
    public ResponseEntity<?> getChatRoom(@PathVariable String roomId)
    {
        long startTime = System.currentTimeMillis();
        log.info("Entering method getChatRoom | params={roomId={}}", roomId);
        try {
            ChatRoomDetailsResponse response = chatRoomService.getChatRoom(roomId);
            log.info("Exiting method getChatRoom | outcome=success | duration={}ms",
                    System.currentTimeMillis() - startTime);
            return ResponseEntity.status(StatusCode.OK).body(response);
        }
        catch (ChatRoomNotFoundException e) {
            log.error("Exception in method getChatRoom | error={}", e.getMessage());
            return ResponseEntity.status(StatusCode.NOT_FOUND).body(StatusResponse.error(e.getMessage()));
        }
        catch (Exception e) {
            log.error("Exception in method getChatRoom | error={}", e.getMessage(), e);
            return ResponseEntity.status(StatusCode.EXCEPTION).body(StatusResponse.error(Messages.FAILED));
        }
    }

    @GetMapping
    @Operation(summary = "List chat rooms")
    public ResponseEntity<Map<String, Set<String>>> listChatRooms()
    {
        log.info("Entering method listChatRooms");
        return ResponseEntity.status(StatusCode.OK).body(Map.of("rooms", chatRoomService.listChatRooms()));
    }

    @DeleteMapping("/{roomId}")
    @Operation(summary = "Delete a chat room")
    public ResponseEntity<StatusResponse> deleteChatRoom(@PathVariable String roomId)
    {
        long startTime = System.currentTimeMillis();
        log.info("Entering method deleteChatRoom | params={roomId={}}", roomId);
        try {
            StatusResponse response = chatRoomService.deleteChatRoom(roomId);
            log.info("Exiting method deleteChatRoom | outcome=success | duration={}ms",
                    System.currentTimeMillis() - startTime);
            return ResponseEntity.status(StatusCode.OK).body(response);
        }
        catch (ChatRoomNotFoundException e) {
            log.error("Exception in method deleteChatRoom | error={}", e.getMessage());
            return ResponseEntity.status(StatusCode.NOT_FOUND).body(StatusResponse.error(e.getMessage()));
        }
        catch (Exception e) {
            log.error("Exception in method deleteChatRoom | error={}", e.getMessage(), e);
            return ResponseEntity.status(StatusCode.EXCEPTION).body(StatusResponse.error(Messages.FAILED));
        }
    }
}
