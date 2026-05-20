package com.neuon.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class ResponseParser {

    private String stopReason;
    private String textContent;
    private JsonArray toolCalls; // raw tool_calls array if present
    private JsonObject rawMessage; // the full assistant message, for history

    public ResponseParser parse(String responseBody) {
        System.err.println("RAW: " + responseBody);
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray choicesArray = root.getAsJsonArray("choices");
            if (choicesArray == null || choicesArray.size() == 0) {
                this.stopReason = "error";
                this.textContent = "[ERROR] No choices in response";
                return this;
            }
            JsonObject choice = choicesArray.get(0).getAsJsonObject();

            JsonElement finishReasonEl = choice.get("finish_reason");
            if (finishReasonEl != null && !finishReasonEl.isJsonNull()) {
                try {
                    this.stopReason = finishReasonEl.getAsString();
                } catch (UnsupportedOperationException e) {
                    this.stopReason = finishReasonEl.toString();
                }
            }

            this.rawMessage = choice.getAsJsonObject("message");
            if (this.rawMessage == null) {
                this.textContent = "[ERROR] No message in choice";
                return this;
            }

            // Text response
            if (rawMessage.has("content") && !rawMessage.get("content").isJsonNull()) {
                JsonElement contentEl = rawMessage.get("content");
                try {
                    this.textContent = contentEl.getAsString();
                } catch (UnsupportedOperationException e) {
                    this.textContent = contentEl.toString();
                }
            }

            // Tool call response
            if (rawMessage.has("tool_calls") && !rawMessage.get("tool_calls").isJsonNull()) {
                this.toolCalls = rawMessage.getAsJsonArray("tool_calls");
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.stopReason = "error";
            this.textContent = "[ERROR] Parse error: " + e.getMessage();
        }

        return this;
    }

    public boolean isToolCall() {
        return "tool_calls".equals(stopReason);
    }

    public boolean isDone() {
        return "stop".equals(stopReason);
    }

    public String getText() { return textContent; }
    public JsonArray getToolCalls() { return toolCalls; }
    public JsonObject getRawMessage() { return rawMessage; } // needed to append to history
    
    // Helper: extract name and arguments from a single tool call
    public static String getToolName(JsonObject toolCall) {
        try {
            JsonObject function = toolCall.getAsJsonObject("function");
            if (function == null) return "";
            JsonElement nameEl = function.get("name");
            if (nameEl == null || nameEl.isJsonNull()) return "";
            return nameEl.getAsString();
        } catch (Exception e) {
            return "";
        }
    }

    public static JsonObject getToolArgs(JsonObject toolCall) {
        try {
            JsonObject function = toolCall.getAsJsonObject("function");
            if (function == null) return new JsonObject();
            JsonElement argsEl = function.get("arguments");
            if (argsEl == null || argsEl.isJsonNull()) return new JsonObject();
            String argsString = argsEl.getAsString();
            return JsonParser.parseString(argsString).getAsJsonObject(); // args come as a string, parse it
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    public static String getToolCallId(JsonObject toolCall) {
        try {
            JsonElement idEl = toolCall.get("id");
            if (idEl == null || idEl.isJsonNull()) return "";
            return idEl.getAsString();
        } catch (Exception e) {
            return "";
        }
    }
}