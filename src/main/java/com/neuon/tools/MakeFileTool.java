package com.neuon.tools;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.gson.JsonObject;

import java.io.IOException;

public class MakeFileTool implements ToolHandler{

    @Override
    public String execute(JsonObject parameters){
        String filePath = parameters.get("file_path").getAsString();
        String fileContent = parameters.get("file_content").getAsString();
        try{
            Files.write(Paths.get(filePath), fileContent.getBytes(StandardCharsets.UTF_8));
            return "file made succefully in '" + filePath+ "'";
        }
        catch(IOException ioe){
            return ioe.getStackTrace().toString();
        }
    }
}
