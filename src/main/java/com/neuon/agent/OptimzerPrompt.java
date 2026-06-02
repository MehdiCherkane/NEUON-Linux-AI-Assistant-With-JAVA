package com.neuon.agent;

import java.util.ArrayList;
import java.util.Collection;

public class OptimzerPrompt {
    private ArrayList<String> tools;
    private String system = """

        You are a tool router. Your job is to analyze a user's message and return the minimal set of tools that a larger AI assistant might need to fulfill the request.

        You will be given a list of available tools with their names and descriptions.
        You must return ONLY a valid JSON object in this exact format:
        {
        "reasoning": "<brief explanation>",
        "tools": ["tool_name_1", "tool_name_2"]
        }

        Rules:
        - Only include tools from the provided list. Never invent tool names.
        - Return an empty array if no tools are needed (e.g. for conversational messages).
        - Prefer fewer tools. When in doubt, leave it out.
        - Do not include any text outside the JSON object.

        Available tools:
        { %s }

            """;

    public OptimzerPrompt(Collection<String> tools) {
        this.tools = new ArrayList<>(tools);
    }

    public OptimzerPrompt() {
        this.tools = new ArrayList<>();
    }

    public String getSystemPrompt(){
        StringBuilder s = new StringBuilder();
        for (String toolName : tools) {
            s.append(toolName + ".\n");
        }
        return this.system.formatted(s);
    }
}
