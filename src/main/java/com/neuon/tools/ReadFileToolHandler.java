package com.neuon.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;

public class ReadFileToolHandler implements ToolHandler{
    @Override
    public String execute(JsonObject parameters){
        try{
            Path filePath = Path.of(parameters.get("file_path").getAsString());
            String fileContent = Files.readString(filePath);
            return fileContent;
        }
        catch(IOException ioe){
            ioe.printStackTrace();
            return "An error occured while finding the file or while reading it!!";
        }

    }
}
