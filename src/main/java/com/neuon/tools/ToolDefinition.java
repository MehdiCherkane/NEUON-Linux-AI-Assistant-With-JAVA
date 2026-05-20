package com.neuon.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ToolDefinition {
    private String name;
    private String description;
    private JsonObject parameters;

    public ToolDefinition(String name, String description) {
        this.name = name;
        this.description = description;

        JsonObject schema = new JsonObject();
        schema.addProperty("type","object");
        schema.add("properties", new JsonObject());
        schema.add("required", new JsonArray());
        this.parameters = schema;

    }

    public ToolDefinition addParameter(String paraName, String type, boolean required){
        JsonObject props = parameters.getAsJsonObject("properties");
        JsonObject newParam = new JsonObject();
        newParam.addProperty("type", type);
        props.add(paraName, newParam);

        if (required) {
            JsonArray requiredParams = parameters.getAsJsonArray("required");
            requiredParams.add(paraName);
        }

        return this;

    }
    public JsonObject toJson() {
        JsonObject tool = new JsonObject();
        tool.addProperty("type", "function");

        JsonObject function = new JsonObject();
        function.addProperty("name", name);
        function.addProperty("description", description);
        function.add("parameters", parameters);

        tool.add("function", function);
        return tool;
    }

    public String getName() { 
        return name; 
    }
}
