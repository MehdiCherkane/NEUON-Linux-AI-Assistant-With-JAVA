package com.neuon.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class MessageBuilder {

    private JsonArray messages = new JsonArray();

    public MessageBuilder addSystem(String content) {
        messages.add(buildMessage("system", content));
        return this;
    }

    public MessageBuilder addUser(String content) {
        messages.add(buildMessage("user", content));
        return this;
    }

    public MessageBuilder addAssistant(String content) {
        messages.add(buildMessage("assistant", content));
        return this;
    }

    // For injecting a tool result back into the conversation
    public MessageBuilder addToolResult(String toolCallId, String result) {
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "tool");
        msg.addProperty("tool_call_id", toolCallId);
        msg.addProperty("content", result);
        messages.add(msg);
        return this;
    }

    // For injecting the assistant's tool_call message back into history
    public MessageBuilder addRaw(JsonObject rawMessage) {
        messages.add(rawMessage);
        return this;
    }

    public JsonArray build() {
        return messages;
    }

    private JsonObject buildMessage(String role, String content) {
        JsonObject msg = new JsonObject();
        msg.addProperty("role", role);
        msg.addProperty("content", content);
        return msg;
    }
}
