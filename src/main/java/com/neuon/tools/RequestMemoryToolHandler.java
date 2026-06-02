package com.neuon.tools;

import com.neuon.core.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
public class RequestMemoryToolHandler implements ToolHandler {
    private Memory memory = new Memory();
    @Override
    public String execute(JsonObject parameters){
        try {
            JsonElement memoriesEl = parameters.get("requested_memories");
            if (memoriesEl == null || memoriesEl.isJsonNull()) {
                return "[ERROR] Missing 'requested_memories' parameter";
            }
            String requestedMemories = memoriesEl.getAsString();
            return memory.requestMemories(requestedMemories);
        } catch (Exception e) {
            return "[ERROR] " + e.getMessage();
        }
    }
    
}
