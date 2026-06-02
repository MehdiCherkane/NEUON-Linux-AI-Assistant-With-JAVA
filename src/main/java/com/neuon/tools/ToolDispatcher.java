package com.neuon.tools;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.JsonObject;

public class ToolDispatcher {
    private HashMap<String, ToolHandler> handlers = new HashMap<>();

    public ToolDispatcher register(String toolName, ToolHandler handler){
        handlers.put(toolName, handler);
        return this;
    }

    public String dispatch(String toolName, JsonObject parameters){
        ToolHandler handler = handlers.get(toolName);
        if (handler != null) {
            return handler.execute(parameters);
        }
        return "Error: this tool '" + toolName + "' is unknown"; 
    }

    public ArrayList<String> getAllToolsNames(){
        return new ArrayList<>(handlers.keySet());
    }
}
