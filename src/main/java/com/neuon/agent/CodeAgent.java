package com.neuon.agent;

import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.neuon.UI.FXInterface;
import com.neuon.core.LLMClient;
import com.neuon.core.Memory;
import com.neuon.core.MessageBuilder;
import com.neuon.core.RequestBuilder;
import com.neuon.core.ResponseParser;
import com.neuon.tools.ToolRunner;
import com.neuon.tools.ToolWareHouse;

public class CodeAgent {

    private LLMClient client = new LLMClient();
    private Memory memory = new Memory();
    private PromptCodeAgent sysPrompt = new PromptCodeAgent();
    private FXInterface userInterface = new FXInterface();
    private ToolWareHouse toolWareHouse = new ToolWareHouse();
    private ToolRunner toolRunner;
    private final ArrayList<String> codeAgentTools = new ArrayList<>(); 

    public CodeAgent() {
        toolRunner = new ToolRunner();
        codeAgentTools.add("make_project_directory");
        codeAgentTools.add("make_file");
        codeAgentTools.add("run_shell");
        codeAgentTools.add("edit_file"); 
    }

    public String startCoding(String promptFromNeuon) {
        try {
            return runAgent(promptFromNeuon);
        } catch (Exception e) {
            return "[ERROR] " + e.toString();
        }
    }

    private String runAgent(String userPrompt) throws Exception {
        
        MessageBuilder messageBuilder = new MessageBuilder(); 

        JsonArray tools = toolWareHouse.getNeededTools(codeAgentTools).toJson();  

        // I add system prompt
        messageBuilder.addSystem(sysPrompt.getPrompt());

        // Past converation histoty
        for (String[] pair : memory.loadShortMemory()) {
            messageBuilder.addUser(pair[0]);
            messageBuilder.addAssistant(pair[1]);
        }

        // current user message
        messageBuilder.addUser(userPrompt);

        int maxSteps = 15;
        int steps = 0;

        while (steps++ < maxSteps) {
            userInterface.sendOutput("loop run %d times".formatted(steps));

            JsonObject body = RequestBuilder.build(messageBuilder.build(), tools, "openai/gpt-oss-120b");
            String raw = client.ask(body);
            ResponseParser parser = new ResponseParser().parse(raw);

            messageBuilder.addRaw(parser.getRawMessage());

            if (parser.isDone()) {
                return parser.getText();
            }

            if (parser.isToolCall()) {
                
                JsonArray toolCalls = parser.getToolCalls();
                for (int i = 0; i < toolCalls.size(); i++) {
                    JsonObject toolCall = toolCalls.get(i).getAsJsonObject();
                    String name = ResponseParser.getToolName(toolCall);
                    JsonObject args = ResponseParser.getToolArgs(toolCall);
                    String id = ResponseParser.getToolCallId(toolCall);
                    String result = toolRunner.execute(name, args);
                    messageBuilder.addToolResult(id, result);
                }
                // loop continues, sends results back to LLM
            }
        }

        return "[ERROR] max steps reached";
    }
}
