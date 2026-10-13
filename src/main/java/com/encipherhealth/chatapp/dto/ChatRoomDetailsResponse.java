package com.encipherhealth.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDetailsResponse
{
    private String roomId;
    private String roomName;
    private String createdAt;
    private Set<String> participants;
}
