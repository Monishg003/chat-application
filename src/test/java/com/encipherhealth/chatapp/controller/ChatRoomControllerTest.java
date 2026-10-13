package com.encipherhealth.chatapp.controller;

import com.encipherhealth.chatapp.dto.ChatHistoryResponse;
import com.encipherhealth.chatapp.dto.ChatMessageDto;
import com.encipherhealth.chatapp.dto.StatusResponse;
import com.encipherhealth.chatapp.exception.ChatRoomNotFoundException;
import com.encipherhealth.chatapp.exception.DuplicateChatRoomException;
import com.encipherhealth.chatapp.exception.GlobalExceptionHandler;
import com.encipherhealth.chatapp.pubsub.ChatMessageSubscriber;
import com.encipherhealth.chatapp.service.ChatRoomService;
import com.encipherhealth.chatapp.util.Messages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatRoomController.class)
@Import(GlobalExceptionHandler.class)
class ChatRoomControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatRoomService chatRoomService;

    @MockitoBean
    private ChatMessageSubscriber chatMessageSubscriber;

    @Test
    void createChatRoomReturnsAssignmentPayload()
            throws Exception
    {
        when(chatRoomService.createChatRoom("general"))
                .thenReturn(StatusResponse.success(Messages.chatRoomCreated("general"), "general"));

        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomName":"general"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.roomId").value("general"))
                .andExpect(jsonPath("$.message").value("Chat room 'general' created successfully."));
    }

    @Test
    void createChatRoomReturnsConflictForDuplicate()
            throws Exception
    {
        when(chatRoomService.createChatRoom("general"))
                .thenThrow(new DuplicateChatRoomException(Messages.chatRoomAlreadyExists("general")));

        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomName":"general"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Chat room 'general' already exists."));
    }

    @Test
    void joinChatRoomReturnsAssignmentPayload()
            throws Exception
    {
        when(chatRoomService.joinChatRoom("general", "guest_user"))
                .thenReturn(StatusResponse.success(Messages.userJoined("guest_user", "general")));

        mockMvc.perform(post("/api/chatapp/chatrooms/general/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participant":"guest_user"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("User 'guest_user' joined chat room 'general'."));
    }

    @Test
    void sendMessageToMissingRoomReturnsError()
            throws Exception
    {
        when(chatRoomService.sendMessage("missing", "guest_user", "Hello, everyone!"))
                .thenThrow(new ChatRoomNotFoundException(Messages.chatRoomDoesNotExist("missing")));

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
    void getMessagesReturnsHistory()
            throws Exception
    {
        when(chatRoomService.getMessages(eq("general"), anyInt()))
                .thenReturn(new ChatHistoryResponse(List.of(
                        new ChatMessageDto("guest_user", "Hello, everyone!", "2024-01-01T10:00:00Z")
                )));

        mockMvc.perform(get("/api/chatapp/chatrooms/general/messages").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].participant").value("guest_user"))
                .andExpect(jsonPath("$.messages[0].message").value("Hello, everyone!"));
    }

    @Test
    void createChatRoomValidatesBlankName()
            throws Exception
    {
        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomName":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }
}
