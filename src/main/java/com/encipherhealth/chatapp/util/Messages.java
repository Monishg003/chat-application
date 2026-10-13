package com.encipherhealth.chatapp.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Messages
{
    public static final String SUCCESS = "success";
    public static final String ERROR = "error";
    public static final String FAILED = "Failed!!";

    public static String chatRoomCreated(String roomId)
    {
        return "Chat room '" + roomId + "' created successfully.";
    }

    public static String userJoined(String participant, String roomId)
    {
        return "User '" + participant + "' joined chat room '" + roomId + "'.";
    }

    public static String messageSent()
    {
        return "Message sent successfully.";
    }

    public static String chatRoomDeleted(String roomId)
    {
        return "Chat room '" + roomId + "' deleted successfully.";
    }

    public static String chatRoomAlreadyExists(String roomId)
    {
        return "Chat room '" + roomId + "' already exists.";
    }

    public static String chatRoomDoesNotExist(String roomId)
    {
        return "Chat room '" + roomId + "' does not exist.";
    }

    public static String userNotJoined(String participant, String roomId)
    {
        return "User '" + participant + "' has not joined chat room '" + roomId + "'.";
    }

    public static String invalidLimit()
    {
        return "Limit must be a positive number.";
    }
}
