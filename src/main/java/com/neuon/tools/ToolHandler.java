package com.neuon.tools;

import com.google.gson.JsonObject;

public interface ToolHandler {
    String execute(JsonObject parameters);
}
