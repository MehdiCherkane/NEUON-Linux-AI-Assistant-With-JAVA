package com.neuon.tools;

import com.google.gson.JsonObject;

public class ListFilesTool implements ToolHandler {

    @Override
    public String execute(JsonObject parameters) {
        if (parameters == null || !parameters.has("path") || parameters.get("path").isJsonNull()) {
            return "Error: missing 'path' parameter";
        }
        String path = parameters.get("path").getAsString().trim();
        if (path.isEmpty()) {
            return "Error: empty path";
        }
        return WorkspaceManager.listFiles(path);
    }
}
