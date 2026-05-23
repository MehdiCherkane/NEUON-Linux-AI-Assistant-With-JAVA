package com.neuon.tools;

import com.google.gson.JsonObject;

public class MakeDirectoryTool implements ToolHandler{
    private Runner runner = new Runner();
    private String currentDir = "/home/mehdi-cherkane/Desktop/workspace/";
    
    @Override
    public String execute(JsonObject parameters){
        String dirPath = parameters.get("directory_path").getAsString();
        ProcessResult result = runner.execute("mkdir "+ currentDir + dirPath);
        if (result.getStderr().isEmpty()) {
            return "directory made succefully in '" + dirPath + "'";
        }
        return result.getStderr();
    }
}