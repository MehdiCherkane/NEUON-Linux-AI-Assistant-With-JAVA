package com.neuon.agent;

import com.neuon.core.*;
import com.neuon.UI.FXInterface;
import com.neuon.tools.ToolRunner;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public class AgentLoop {

    private static final String MODEL = System.getenv("LLM_MODEL") != null ? System.getenv("LLM_MODEL") : "openai/gpt-oss-120b";

    private final LLMClient client;
    private final FXInterface userInterface;
    private final ToolRunner toolRunner;
    private final int maxSteps;

    public AgentLoop(ToolRunner toolRunner, FXInterface userInterface) {
        this.client = new LLMClient();
        this.userInterface = userInterface;
        this.toolRunner = toolRunner;
        this.maxSteps = 15;
    }

    public AgentLoop(ToolRunner toolRunner, FXInterface userInterface, int maxSteps) {
        this.client = new LLMClient();
        this.userInterface = userInterface;
        this.toolRunner = toolRunner;
        this.maxSteps = maxSteps;
    }

    public String run(String systemPrompt, JsonArray tools, String userMessage, List<String[]> history) {
        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.addSystem(systemPrompt);

        for (String[] pair : history) {
            if (pair.length >= 2) {
                messageBuilder.addUser(pair[0]);
                messageBuilder.addAssistant(pair[1]);
            }
        }

        messageBuilder.addUser(userMessage);

        int steps = 0;
        while (steps++ < maxSteps) {
            if (userInterface != null) {
                userInterface.sendOutput("loop run %d/%d".formatted(steps, maxSteps));
            }

            JsonObject body = RequestBuilder.build(messageBuilder.build(), tools, MODEL);
            LLMResult llmResult = client.ask(body);

            if (!llmResult.ok()) {
                if (llmResult.statusCode() == 429 && userInterface != null) {
                    userInterface.sendOutput("[SYSTEM] Rate limited by API, retrying...");
                }
                return "[ERROR] API request failed: " + llmResult.error();
            }

            ResponseParser parser = new ResponseParser().parse(llmResult.body());

            if (parser.isError()) {
                return parser.getText();
            }

            JsonObject rawMessage = parser.getRawMessage();
            if (rawMessage != null) {
                messageBuilder.addRaw(rawMessage);
            }

            if (parser.isDone()) {
                return parser.getText();
            }

            if (parser.isToolCall()) {
                JsonArray toolCalls = parser.getToolCalls();
                if (toolCalls == null || toolCalls.size() == 0) {
                    return "[ERROR] Tool call finish reason without tool calls";
                }
                for (int i = 0; i < toolCalls.size(); i++) {
                    JsonObject toolCall = toolCalls.get(i).getAsJsonObject();
                    String name = ResponseParser.getToolName(toolCall);
                    JsonObject args = ResponseParser.getToolArgs(toolCall);
                    String id = ResponseParser.getToolCallId(toolCall);
                    if (userInterface != null) {
                        userInterface.setToolCall("→ " + name + "(" + args + ")");
                    }
                    String result = toolRunner.execute(name, args);
                    if (userInterface != null) {
                        String summary = result.length() > 80 ? result.substring(0, 80) + "…" : result;
                        userInterface.setToolCall("✓ " + name + " → " + summary);
                    }
                    messageBuilder.addToolResult(id, result);
                }
            }
        }

        return "[ERROR] max steps reached";
    }
}
