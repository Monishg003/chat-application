package com.encipherhealth.chatapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"message", "roomId", "status"})
public class StatusResponse
{
    private String message;
    private String status;
    private String roomId;

    public static StatusResponse success(String message)
    {
        return new StatusResponse(message, "success", null);
    }

    public static StatusResponse success(String message, String roomId)
    {
        return new StatusResponse(message, "success", roomId);
    }

    public static StatusResponse error(String message)
    {
        return new StatusResponse(message, "error", null);
    }
}
