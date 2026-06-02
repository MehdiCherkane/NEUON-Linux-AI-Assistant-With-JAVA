package com.neuon.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageBuilderTest {

    @Test
    void buildsMessagesInOrder() {
        MessageBuilder b = new MessageBuilder();
        b.addSystem("You are a helpful AI.");
        b.addUser("Hello");
        b.addAssistant("Hi boss!");
        JsonArray msgs = b.build();
        assertEquals(3, msgs.size());
        assertEquals("system", msgs.get(0).getAsJsonObject().get("role").getAsString());
        assertEquals("user", msgs.get(1).getAsJsonObject().get("role").getAsString());
        assertEquals("assistant", msgs.get(2).getAsJsonObject().get("role").getAsString());
    }

    @Test
    void doesNotAddNullRawMessage() {
        MessageBuilder b = new MessageBuilder();
        b.addSystem("system");
        b.addRaw(null);
        assertEquals(1, b.build().size());
    }

    @Test
    void doesNotAddRawMessageWithoutRole() {
        MessageBuilder b = new MessageBuilder();
        b.addSystem("system");
        JsonObject noRole = new JsonObject();
        noRole.addProperty("content", "hello");
        b.addRaw(noRole);
        assertEquals(1, b.build().size());
    }

    @Test
    void doesNotAddRawMessageWithNullRole() {
        MessageBuilder b = new MessageBuilder();
        b.addSystem("system");
        JsonObject nullRole = new JsonObject();
        nullRole.add("role", com.google.gson.JsonNull.INSTANCE);
        nullRole.addProperty("content", "hello");
        b.addRaw(nullRole);
        assertEquals(1, b.build().size());
    }

    @Test
    void addsValidRawMessage() {
        MessageBuilder b = new MessageBuilder();
        b.addSystem("system");
        JsonObject raw = new JsonObject();
        raw.addProperty("role", "assistant");
        raw.addProperty("content", "Hello");
        raw.add("tool_calls", new JsonArray());
        b.addRaw(raw);
        assertEquals(2, b.build().size());
        JsonObject msg = b.build().get(1).getAsJsonObject();
        assertEquals("assistant", msg.get("role").getAsString());
    }

    @Test
    void addsToolResultWithCorrectStructure() {
        MessageBuilder b = new MessageBuilder();
        b.addSystem("system");
        b.addUser("list files");
        b.addToolResult("call_123", "file1.txt\nfile2.txt");
        assertEquals(3, b.build().size());
        JsonObject toolMsg = b.build().get(2).getAsJsonObject();
        assertEquals("tool", toolMsg.get("role").getAsString());
        assertEquals("call_123", toolMsg.get("tool_call_id").getAsString());
        assertEquals("file1.txt\nfile2.txt", toolMsg.get("content").getAsString());
    }
}
