package com.neuon.core;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
    private int contextWindow = 6;
    private final List<String[]> shortTermMemory = Collections.synchronizedList(new ArrayList<>());

    public Memory() {
        String home = System.getProperty("user.home", ".");
        this.pathToLongTermMemory = home + "/.neuon/LongTermMemory.json";
    }

    public Memory(String customPath) {
        this.pathToLongTermMemory = customPath;
    }

    public void updateShortTermMemory(String message, String response){
        if (response != null && response.startsWith("[ERROR]")) {
            return;
        }
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
        JsonObject root = safeParse();
        if (root == null) return "";
        try {
            JsonObject categories = root.getAsJsonObject("_categories_index");
            if (categories == null) {
                categories = new JsonObject();
                root.add("_categories_index", categories);
            }

            boolean indexUpdated = false;
            // Fallback: scan for unindexed category arrays (legacy repair)
            for (String key : root.keySet()) {
                if (key.equals("_categories_index")) continue;
                if (!categories.has(key)) {
                    JsonElement val = root.get(key);
                    if (val != null && val.isJsonArray() && val.getAsJsonArray().size() > 0) {
                        categories.addProperty(key, "User saved memories about " + key);
                        indexUpdated = true;
                    }
                }
            }

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

            if (indexUpdated) {
                try (FileWriter writer = new FileWriter(pathToLongTermMemory)) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                    gson.toJson(root, writer);
                } catch (IOException ignored) {}
            }

            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public String updateLongTermMemory(String memoriesCategory, String newMemory){
        JsonObject root = safeParse();
        if (root == null) {
            root = new JsonObject();
            root.add("_categories_index", new JsonObject());
        }
        try {
            JsonObject index = root.getAsJsonObject("_categories_index");
            if (index == null) {
                index = new JsonObject();
                root.add("_categories_index", index);
            }

            JsonArray array = root.getAsJsonArray(memoriesCategory);
            if (array == null) {
                array = new JsonArray();
                root.add(memoriesCategory, array);
                index.addProperty(memoriesCategory, "User saved memories about " + memoriesCategory);
            }
            array.add("- %s".formatted(newMemory));
            try (FileWriter writer = new FileWriter(pathToLongTermMemory)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                gson.toJson(root, writer);
            } catch (IOException e) {
                return "Error writing to memory file.";
            }
            return "Long memory updated successfully.";
        } catch (Exception e) {
            return "Error updating memory: " + e.getMessage();
        }
    }

    public String requestMemories(String categoryName){
        ensureFileExists();
        JsonObject root = safeParse();
        if (root == null) {
            return "Error: memory file is empty or corrupted.";
        }
        try {
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
            StringBuilder requestedMemories = new StringBuilder();
            for (JsonElement element : array) {
                requestedMemories.append(element);
                requestedMemories.append("\n");
            }
            return requestedMemories.toString();
        } catch (Exception e) {
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
        }
    }

    private JsonObject safeParse() {
        ensureFileExists();
        try (FileReader reader = new FileReader(pathToLongTermMemory)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            try {
                File f = new File(pathToLongTermMemory);
                // Backup corrupt file before resetting to prevent data loss
                File backup = new File(pathToLongTermMemory + ".bak");
                if (f.exists()) {
                    Files.copy(f.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                try (FileWriter w = new FileWriter(f)) {
                    w.write("{\"_categories_index\": {}}");
                }
                try (FileReader reader = new FileReader(pathToLongTermMemory)) {
                    return JsonParser.parseReader(reader).getAsJsonObject();
                }
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
