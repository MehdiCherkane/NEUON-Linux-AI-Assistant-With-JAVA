package com.neuon.core;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Memory {
    private String pathToLongTermMemory;
    private int contextWindow = 8;
    private final List<String[]> shortTermMemory = Collections.synchronizedList(new ArrayList<>());

    public Memory() {
        String home = System.getProperty("user.home", ".");
        this.pathToLongTermMemory = home + "/.neuon/LongTermMemory.json";
    }

    public Memory(String customPath) {
        this.pathToLongTermMemory = customPath;
    }

    public void updateShortTermMemory(String message, String response){
        synchronized (shortTermMemory) {
            if (shortTermMemory.size() >= contextWindow) {
                shortTermMemory.remove(0);
            }
            shortTermMemory.add(new String[]{message, response});
        }
    }

    public List<String[]> loadShortMemory(){
        synchronized (shortTermMemory) {
            return new ArrayList<>(shortTermMemory);
        }
    }

    public void clearShortMemory(){
        synchronized (shortTermMemory) {
            shortTermMemory.clear();
        }
    }

    public String getMemoriesCategories(){
        try (FileReader reader = new FileReader(pathToLongTermMemory)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject categories = root.getAsJsonObject("_categories_index");
            if (categories == null) return "";
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
            return "";
        }
    }

    public String updateLongTermMemory(String memoriesCategory, String newMemory){
        JsonObject root;
        ensureFileExists();
        try (FileReader reader = new FileReader(pathToLongTermMemory)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            return "Error reading memory file.";
        }
        JsonArray array = root.getAsJsonArray(memoriesCategory);
        if (array == null) {
            array = new JsonArray();
            root.add(memoriesCategory, array);
        }
        array.add("- %s".formatted(newMemory));
        try (FileWriter writer = new FileWriter(pathToLongTermMemory)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            gson.toJson(root, writer);
        } catch (IOException e) {
            return "Error writing to memory file.";
        }
        return "Long memory updated successfully.";
    }

    public String requestMemories(String categoryName){
        StringBuilder requestedMemories = new StringBuilder();
        ensureFileExists();
        try (FileReader reader = new FileReader(pathToLongTermMemory)) {
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
            return requestedMemories.toString();
        } 
        catch (Exception e) {
            return "Error retrieving memory: " + e.getMessage();
        }
    }

    private void ensureFileExists() {
        try {
            File f = new File(pathToLongTermMemory);
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                try (FileWriter w = new FileWriter(f)) {
                    w.write("{\"_categories_index\": {}}");
                }
            }
        } catch (IOException e) {
            // ignore
        }
    }
}