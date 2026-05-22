package com.neuon.agent;

import com.neuon.core.*;
import com.neuon.tools.*;
import com.neuon.UI.*;
import com.google.gson.*;

public class OrchesterAgent {

    private LLMClient client = new LLMClient();
    private Memory memory = new Memory();
    private PromptOrchester sysPrompt = new PromptOrchester();
    private FXInterface userInterface = new FXInterface();
    private ToolWareHouse toolWareHouse = new ToolWareHouse();

    private ToolDispatcher toolDispatcher;
    private ToolRunner toolRunner;
    private CodeAgent codeAgent;

    public OrchesterAgent() {
        
        this.toolDispatcher = new ToolDispatcher();
        this.toolRunner = new ToolRunner(toolDispatcher);
        this.codeAgent = new CodeAgent(toolRunner);

        toolDispatcher
            .register("run_shell", new ShellTool())
            .register("invoke_code_agent", new CodeToolHandler(codeAgent))
            .register("update_long_term_memory", new LongMemoryToolHandler())
            .register("find_on_youtube", new YouTubeToolHandler())
            .register("exit_Neuon", new ExitToolHandler())
            .register("request_memories", new RequestMemoryToolHandler())
            .register("read_file", new ReadFileToolHandler())
            .register("send_email", new EmailTool());
    }
    

    public String getLLMResponse(String userPrompt) {
        try {
            String finalResponse = runConversation(userPrompt);
            memory.updateShortTermMemory(userPrompt, finalResponse);
            return finalResponse;
        } catch (Exception e) {
            return "[ERROR] " + e.toString();
        }
    }

    private String runConversation(String userPrompt) throws Exception {
        
        MessageBuilder messageBuilder = new MessageBuilder(); // fresh every call

        // optimization in case of many tools, now we don't have much
        /* JsonArray tools = toolWareHouse.getNeededTools(userPrompt).toJson(); */

        JsonArray tools = toolWareHouse.getAllTools().toJson();

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
