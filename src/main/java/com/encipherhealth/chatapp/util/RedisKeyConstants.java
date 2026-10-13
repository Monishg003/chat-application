package com.encipherhealth.chatapp.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RedisKeyConstants
{
    public static final String ROOM_INDEX_KEY = "chat:rooms";
    public static final String ROOM_KEY_PREFIX = "chat:room:";
    public static final String PARTICIPANTS_SUFFIX = ":participants";
    public static final String MESSAGES_SUFFIX = ":messages";
    public static final String LIVE_CHAT_CHANNEL_PREFIX = "channel:live-chat:";
    public static final String LIVE_CHAT_CHANNEL_PATTERN = "channel:live-chat:*";
    public static final String HASH_FIELD_ROOM_NAME = "roomName";
    public static final String HASH_FIELD_CREATED_AT = "createdAt";

    public static String roomKey(String roomId)
    {
        return ROOM_KEY_PREFIX + roomId;
    }

    public static String participantsKey(String roomId)
    {
        return ROOM_KEY_PREFIX + roomId + PARTICIPANTS_SUFFIX;
    }

    public static String messagesKey(String roomId)
    {
        return ROOM_KEY_PREFIX + roomId + MESSAGES_SUFFIX;
    }

    public static String liveChatChannel(String roomId)
    {
        return LIVE_CHAT_CHANNEL_PREFIX + roomId;
    }

    public static String roomIdFromChannel(String channel)
    {
        if (channel == null || !channel.startsWith(LIVE_CHAT_CHANNEL_PREFIX)) {
            return channel;
        }
        return channel.substring(LIVE_CHAT_CHANNEL_PREFIX.length());
    }
}
