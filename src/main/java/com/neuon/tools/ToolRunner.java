package com.neuon.tools;

import com.google.gson.JsonObject;

public class ToolRunner {
    private ToolDispatcher dispatcher = new ToolDispatcher();
    public String execute(String getToolName, JsonObject args){
        return dispatcher.dispatch(getToolName, args);
    }
    
}
