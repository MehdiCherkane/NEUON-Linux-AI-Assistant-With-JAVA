package com.neuon.core;
import com.neuon.UI.*;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Memory {
    private Interface userInterface = new FXInterface();
    private String pathToLongMemeory = "src/main/resources/LongTermMemory.json";

    private int contextWindow = 8; // the number of last interactions to keep in short term memory
    private ArrayList<String[]> shortTermMemory = new ArrayList<>();

    // add new pair of (userPrompt, LLMresponse) with respect to context window.
    public void updateShortTermMemory(String message, String response){
        if (shortTermMemory.size() >= contextWindow) {
            shortTermMemory.remove(0);
        }
        shortTermMemory.add(new String[]{message, response});
    }

    // get short memory (it will be sent to the LLM)
    public ArrayList<String[]> loadShortMemory(){
        return shortTermMemory;
    }

    // clearing our memory intetionally (even if don't close program)
    public void clearShortMemory(){
        shortTermMemory.clear();
    }

    public String getMemoriesCategories(){

        try (FileReader reader = new FileReader(pathToLongMemeory)) {

            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject categories = root.getAsJsonObject("_categories_index");
            StringBuilder sb = new StringBuilder();
            for (String key : categories.keySet()) {
                String desc = "";
                JsonElement el = categories.get(key);
                if (el != null && !el.isJsonNull()) {
                    try {
                        desc = el.getAsString();
                    } catch (UnsupportedOperationException ex) {
                        desc = el.toString();
                    }
                }
                sb.append("- ").append(key).append(": ").append(desc).append("\n");
            }
            return sb.toString();
        } 
        catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public String updateLongTermMemory(String memoriesCategory, String newMemory){
        JsonObject root;

        // 1. READ PHASE (Scoped separately so the file is released immediately)
        try (FileReader reader = new FileReader(pathToLongMemeory)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error reading memory file.";
        }

        // 2. MODIFY PHASE
        JsonArray array = root.getAsJsonArray(memoriesCategory);
        
        // Safety check: if category doesn't exist, create it!
        if (array == null) {
            array = new JsonArray();
            root.add(memoriesCategory, array);
        }
        
        array.add("- %s".formatted(newMemory));

        // 3. WRITE PHASE
        try (FileWriter writer = new FileWriter(pathToLongMemeory)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            gson.toJson(root, writer);
        } catch (IOException e) {
            e.printStackTrace();
            return "Error writing to memory file.";
        }

        userInterface.sendOutput("-> Long term memory updated.");
        return "Long memory updated successfully.";
    }

    public String requestMemories(String categoryName){

        StringBuilder requestedMemories = new StringBuilder();

        try (FileReader reader = new FileReader(pathToLongMemeory)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has(categoryName)) {
                return "Category '" + categoryName + "' not found in memory.";
            }
            JsonElement categoryEl = root.get(categoryName);
            if (categoryEl == null || categoryEl.isJsonNull()) {
                return "Category '" + categoryName + "' is null.";
            }
            if (!categoryEl.isJsonArray()) {
                return "Category '" + categoryName + "' is not an array.";
            }
            JsonArray array = categoryEl.getAsJsonArray();
            for (JsonElement element : array) {
                requestedMemories.append(element);
                requestedMemories.append("\n");
            }
            userInterface.sendOutput("-> Memory requested!");
            return requestedMemories.toString();
        } 
        catch (Exception e) {
            e.printStackTrace();
            return "Error retrieving memory: " + e.getMessage();
        }
    }
}