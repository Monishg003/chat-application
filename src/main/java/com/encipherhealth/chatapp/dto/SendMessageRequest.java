package com.encipherhealth.chatapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest
{
    @NotBlank(message = "participant is required")
    private String participant;

    @NotBlank(message = "message is required")
    private String message;
}
