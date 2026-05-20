package com.neuon.tools;

import com.neuon.core.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
public class LongMemoryToolHandler implements ToolHandler {
    private Memory memory = new Memory();
    @Override
    public String execute(JsonObject parameters) {
        try {
            JsonElement rememberEl = parameters.get("something_to_remember");
            JsonElement categoryEl = parameters.get("memory_category");
            
            if (rememberEl == null || rememberEl.isJsonNull() || categoryEl == null || categoryEl.isJsonNull()) {
                return "[ERROR] Missing required parameters";
            }
            
            String newMemory = rememberEl.getAsString();
            String memoryCategory = categoryEl.getAsString();
            return memory.updateLongTermMemory(memoryCategory, newMemory);
        } catch (Exception e) {
            e.printStackTrace();
            return "[ERROR] " + e.getMessage();
        }
    }
}
