package com.encipherhealth.chatapp.service;

import com.encipherhealth.chatapp.dto.ChatHistoryResponse;
import com.encipherhealth.chatapp.dto.ChatMessageDto;
import com.encipherhealth.chatapp.dto.StatusResponse;
import com.encipherhealth.chatapp.exception.ChatRoomNotFoundException;
import com.encipherhealth.chatapp.exception.CustomException;
import com.encipherhealth.chatapp.exception.DuplicateChatRoomException;
import com.encipherhealth.chatapp.exception.ParticipantNotJoinedException;
import com.encipherhealth.chatapp.repository.ChatRoomRepository;
import com.encipherhealth.chatapp.util.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest
{
    @Mock
    private ChatRoomRepository chatRoomRepository;

    @InjectMocks
    private ChatRoomServiceImpl chatRoomService;

    @BeforeEach
    void setUp()
    {
        chatRoomService = new ChatRoomServiceImpl(chatRoomRepository);
    }

    @Test
    void createChatRoomStoresUniqueRoom()
    {
        when(chatRoomRepository.createRoom("general")).thenReturn(true);

        StatusResponse response = chatRoomService.createChatRoom(" general ");

        assertEquals("success", response.getStatus());
        assertEquals("general", response.getRoomId());
        assertEquals(Messages.chatRoomCreated("general"), response.getMessage());
        verify(chatRoomRepository).createRoom("general");
    }

    @Test
    void createChatRoomRejectsDuplicateName()
    {
        when(chatRoomRepository.createRoom("general")).thenReturn(false);

        DuplicateChatRoomException exception = assertThrows(DuplicateChatRoomException.class,
                () -> chatRoomService.createChatRoom("general"));

        assertEquals(Messages.chatRoomAlreadyExists("general"), exception.getMessage());
    }

    @Test
    void joinChatRoomAddsParticipant()
    {
        when(chatRoomRepository.exists("general")).thenReturn(true);

        StatusResponse response = chatRoomService.joinChatRoom("general", "guest_user");

        assertEquals("success", response.getStatus());
        assertEquals(Messages.userJoined("guest_user", "general"), response.getMessage());
        verify(chatRoomRepository).addParticipant("general", "guest_user");
    }

    @Test
    void joinChatRoomFailsWhenRoomMissing()
    {
        when(chatRoomRepository.exists("missing")).thenReturn(false);

        assertThrows(ChatRoomNotFoundException.class,
                () -> chatRoomService.joinChatRoom("missing", "guest_user"));
        verify(chatRoomRepository, never()).addParticipant(any(), any());
    }

    @Test
    void sendMessagePersistsAndPublishesInOrder()
    {
        when(chatRoomRepository.exists("general")).thenReturn(true);
        when(chatRoomRepository.isParticipant("general", "guest_user")).thenReturn(true);

        StatusResponse response = chatRoomService.sendMessage("general", "guest_user", "Hello, everyone!");

        assertEquals("success", response.getStatus());
        assertEquals(Messages.messageSent(), response.getMessage());
        ArgumentCaptor<ChatMessageDto> messageCaptor = ArgumentCaptor.forClass(ChatMessageDto.class);
        verify(chatRoomRepository).appendMessage(eq("general"), messageCaptor.capture());
        verify(chatRoomRepository).publish(eq("general"), messageCaptor.capture());
        ChatMessageDto stored = messageCaptor.getAllValues().get(0);
        assertEquals("guest_user", stored.getParticipant());
        assertEquals("Hello, everyone!", stored.getMessage());
    }

    @Test
    void sendMessageRejectsUnknownRoom()
    {
        when(chatRoomRepository.exists("missing")).thenReturn(false);

        ChatRoomNotFoundException exception = assertThrows(ChatRoomNotFoundException.class,
                () -> chatRoomService.sendMessage("missing", "guest_user", "hello"));

        assertEquals(Messages.chatRoomDoesNotExist("missing"), exception.getMessage());
        verify(chatRoomRepository, never()).appendMessage(any(), any());
        verify(chatRoomRepository, never()).publish(any(), any());
    }

    @Test
    void sendMessageRejectsParticipantWhoHasNotJoined()
    {
        when(chatRoomRepository.exists("general")).thenReturn(true);
        when(chatRoomRepository.isParticipant("general", "stranger")).thenReturn(false);

        assertThrows(ParticipantNotJoinedException.class,
                () -> chatRoomService.sendMessage("general", "stranger", "hello"));
    }

    @Test
    void getMessagesReturnsLastNInChronologicalOrder()
    {
        when(chatRoomRepository.exists("general")).thenReturn(true);
        when(chatRoomRepository.getLastMessages("general", 1)).thenReturn(List.of(
                new ChatMessageDto("another_user", "Hi, guest_user!", "2024-01-01T10:01:00Z")
        ));

        ChatHistoryResponse response = chatRoomService.getMessages("general", 1);

        assertEquals(1, response.getMessages().size());
        assertEquals("Hi, guest_user!", response.getMessages().get(0).getMessage());
    }

    @Test
    void getMessagesRejectsInvalidLimit()
    {
        assertThrows(CustomException.class, () -> chatRoomService.getMessages("general", 0));
    }

    @Test
    void deleteChatRoomRemovesKeys()
    {
        when(chatRoomRepository.exists("general")).thenReturn(true);

        StatusResponse response = chatRoomService.deleteChatRoom("general");

        assertEquals("success", response.getStatus());
        verify(chatRoomRepository).deleteRoom("general");
    }
}
