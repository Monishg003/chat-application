package com.encipherhealth.chatapp;

import org.junit.jupiter.api.Test;

class ChatApplicationTests
{
    @Test
    void messagesDescribeAssignmentFlows()
    {
        org.junit.jupiter.api.Assertions.assertEquals(
                "Chat room 'general' created successfully.",
                com.encipherhealth.chatapp.util.Messages.chatRoomCreated("general"));
    }
}
