package com.neuon.tools;

import com.neuon.UI.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
public class CodeToolHandler implements ToolHandler{
    private Interface userInterface = new FXInterface();
    private Runner runner = new Runner();
    private String codeDirectory = "/home/mehdi-cherkane/Desktop/AI_CODE/";

    @Override 
    public String execute(JsonObject parameters){

        String fileName = parameters.get("file_name").getAsString();
        String fileContent = parameters.get("file_content").getAsString();
        String filePath = codeDirectory + fileName;
        // Write to file
        try {
            Files.write(Path.of(filePath), fileContent.getBytes());
            userInterface.sendOutput("Code written to file: " + fileName);
            runner.execute("code " + filePath);
            return "Content written sucessfully to" + fileName;

        } catch (IOException e) {
            return e.getMessage();
        }
    }
}