package com.neuon.tools;

import com.google.gson.JsonObject;

public class ReadFileToolHandler implements ToolHandler {

    @Override
    public String execute(JsonObject parameters) {
        if (parameters == null || !parameters.has("file_path") || parameters.get("file_path").isJsonNull()) {
            return "Error: missing 'file_path' parameter";
        }
        String filePath = parameters.get("file_path").getAsString().trim();
        if (filePath.isEmpty()) {
            return "Error: empty file_path";
        }

        try {
            return WorkspaceManager.readFile(filePath);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
