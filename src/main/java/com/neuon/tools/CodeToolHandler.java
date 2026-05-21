package com.neuon.tools;

import com.neuon.agent.*;

import com.google.gson.JsonObject;
public class CodeToolHandler implements ToolHandler{

    private CodeAgent codeAgent;
    public CodeToolHandler(CodeAgent codeAgent){
        this.codeAgent = codeAgent;
    }
                          
    @Override
    public String execute(JsonObject parameters){
        String prompt = parameters.get("prompt").getAsString();
        return codeAgent.startCoding(prompt);
    }
}