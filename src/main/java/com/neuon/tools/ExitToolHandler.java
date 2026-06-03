package com.neuon.tools;
import com.neuon.core.*;

import com.google.gson.JsonObject;

public class ExitToolHandler implements ToolHandler{
    private Memory memory;

    public ExitToolHandler(Memory memory) {
        this.memory = memory;
    }

    @Override 
    public String execute(JsonObject parameters){
        memory.clearShortMemory();
        System.exit(0);
        return null;
    }

}