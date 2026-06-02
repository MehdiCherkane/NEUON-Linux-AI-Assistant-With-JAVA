package com.neuon.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;

public class MakeDirectoryTool implements ToolHandler{
    
    @Override
    public String execute(JsonObject parameters){
        if (parameters == null || !parameters.has("dir_name") || parameters.get("dir_name").isJsonNull()) {
            return "[ERROR] Missing 'dir_name' parameter";
        }

        String dirName = parameters.get("dir_name").getAsString().replaceAll("[^a-zA-Z0-9_-]", "_");
        if (dirName.isBlank()) {
            return "Invalid directory name.";
        }

        try {
            Path path = WorkspacePaths.resolveInsideWorkspace(dirName);
            Files.createDirectories(path);
            return "Directory made successfully in '" + path + "'";
        } catch (IllegalArgumentException | IOException e) {
            return "Error creating directory: " + e.getMessage();
        }
    }
}
