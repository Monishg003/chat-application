package com.encipherhealth.chatapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatRoomRequest
{
    @NotBlank(message = "roomName is required")
    private String roomName;
}
