package com.neuon.tools;

import com.google.gson.JsonObject;

public class EditFileTool implements ToolHandler {

    @Override
    public String execute(JsonObject parameters) {
        if (parameters == null || !parameters.has("file_path") || parameters.get("file_path").isJsonNull()) {
            return "Error: missing 'file_path' parameter";
        }
        if (!parameters.has("file_content") || parameters.get("file_content").isJsonNull()) {
            return "Error: missing 'file_content' parameter";
        }

        String filePath = parameters.get("file_path").getAsString().trim();
        String fileContent = parameters.get("file_content").getAsString();

        if (filePath.isEmpty()) {
            return "Error: empty file_path";
        }

        return WorkspaceManager.writeFile(filePath, fileContent);
    }
}
