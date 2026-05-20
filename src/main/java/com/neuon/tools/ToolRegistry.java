package com.neuon.tools;

import com.google.gson.JsonArray;
import java.util.ArrayList;
import java.util.List;

public class ToolRegistry {

    private List<ToolDefinition> tools = new ArrayList<>();

    public ToolRegistry register(ToolDefinition tool) {
        tools.add(tool);
        return this; // chainable
    }

    // Returns the full "tools" array to plug into the request
    public JsonArray toJson() {
        JsonArray array = new JsonArray();
        for (ToolDefinition tool : tools) {
            array.add(tool.toJson());
        }
        return array;
    }
}
