package com.neuon.tools;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;

import java.io.IOException;

public class MakeFileTool implements ToolHandler{

    @Override
    public String execute(JsonObject parameters){
        if (parameters == null || !parameters.has("file_path") || parameters.get("file_path").isJsonNull()) {
            return "[ERROR] Missing 'file_path' parameter";
        }
        if (!parameters.has("file_content") || parameters.get("file_content").isJsonNull()) {
            return "[ERROR] Missing 'file_content' parameter";
        }

        String filePath = parameters.get("file_path").getAsString();
        String fileContent = parameters.get("file_content").getAsString();
        String sanitized = filePath.replaceAll("[^a-zA-Z0-9_./ -]", "_");
        try{
            Path path = WorkspacePaths.resolveInsideWorkspace(sanitized);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(path, fileContent.getBytes(StandardCharsets.UTF_8));
            return "File made successfully in '" + path + "'";
        }
        catch(IllegalArgumentException iae){
            return "Error writing file: " + iae.getMessage();
        }
        catch(IOException ioe){
            return "Error writing file: " + ioe.getMessage();
        }
    }
}
