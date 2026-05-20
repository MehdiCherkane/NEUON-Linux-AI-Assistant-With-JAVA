package com.neuon.agent;

import com.neuon.tools.*;
import com.neuon.core.*;

import java.util.ArrayList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ToolOptimzerModel {

    private final String MODEL = "openai/gpt-oss-120b";
    private ToolDispatcher toolDispatcher = new ToolDispatcher();
    private LLMClient optimizer = new LLMClient();
    private ArrayList<String> allToolsNames = toolDispatcher.getAllToolsNames();
    private OptimzerPrompt sysPrompt = new OptimzerPrompt();
    private Memory memory = new Memory();


    // I'm gonna use a small LLM to decide the needed tools.
    public ArrayList<String> getNeededTools(String userPrompt) {
        
        MessageBuilder msg = new MessageBuilder()
                .addSystem(sysPrompt.getSystemPrompt());

        for (String[] pair : memory.loadShortMemory()) {
            msg.addUser(pair[0]);
            msg.addAssistant(pair[1]);
        }

        msg.addUser(userPrompt);

        JsonObject fullMessage = RequestBuilder.build(msg.build(), null, MODEL);
        
        ResponseParser response;
        try{
            response = new ResponseParser().parse(optimizer.ask(fullMessage));
        }
        catch(Exception e){
            return null;
        }

        // the model's JSON response is in getText()
        ArrayList<String> neededTools = new ArrayList<>();
        try {
            JsonObject parsed = JsonParser.parseString(response.getText()).getAsJsonObject();
            JsonArray toolsArray = parsed.getAsJsonArray("tools");
            if (toolsArray == null) return neededTools;

            for (JsonElement el : toolsArray) {
                if (el == null || el.isJsonNull()) continue;
                try {
                    String toolName = el.getAsString();
                    if (allToolsNames.contains(toolName)) {
                        neededTools.add(toolName);
                    }
                } catch (UnsupportedOperationException e) {
                    // Element is not a string, skip it
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return neededTools;
    }
    public static void main(String[] args) {
        ToolOptimzerModel t = new ToolOptimzerModel();
        ArrayList<String> needed = t.getNeededTools("Tell my name, then make me a python program that prints it 60 times");
        for (String tool : needed) {
            System.out.println(tool);
        }
    }
}
