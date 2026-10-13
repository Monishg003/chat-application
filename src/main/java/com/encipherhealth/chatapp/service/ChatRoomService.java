package com.encipherhealth.chatapp.service;

import com.encipherhealth.chatapp.dto.ChatHistoryResponse;
import com.encipherhealth.chatapp.dto.ChatRoomDetailsResponse;
import com.encipherhealth.chatapp.dto.StatusResponse;

import java.util.Set;

public interface ChatRoomService
{
    StatusResponse createChatRoom(String roomName);

    StatusResponse joinChatRoom(String roomId, String participant);

    StatusResponse sendMessage(String roomId, String participant, String message);

    ChatHistoryResponse getMessages(String roomId, Integer limit);

    StatusResponse deleteChatRoom(String roomId);

    ChatRoomDetailsResponse getChatRoom(String roomId);

    Set<String> listChatRooms();
}
