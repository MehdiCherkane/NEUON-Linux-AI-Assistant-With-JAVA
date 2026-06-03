package com.neuon.agent;

import com.neuon.core.*;
import com.neuon.tools.*;
import com.neuon.UI.*;

import com.google.gson.JsonArray;

import java.util.ArrayList;

public class OrchesterAgent {

    private AgentLoop agentLoop;
    private Memory memory;
    private PromptOrchester sysPrompt;
    private ToolDispatcher toolDispatcher;
    private ToolRunner toolRunner;
    private CodeAgent codeAgent;
    private JsonArray orchestorTools;

    public OrchesterAgent(FXInterface fxInterface) {
        this.memory = new Memory();
        this.sysPrompt = new PromptOrchester(memory);
        this.toolDispatcher = new ToolDispatcher();
        this.toolRunner = new ToolRunner(toolDispatcher);
        this.codeAgent = new CodeAgent(toolRunner, fxInterface);
        this.agentLoop = new AgentLoop(toolRunner, fxInterface);

        toolDispatcher
            .register("run_shell", new ShellTool(fxInterface))
            .register("invoke_code_agent", new CodeToolHandler(codeAgent))
            .register("update_long_term_memory", new LongMemoryToolHandler(memory))
            .register("find_on_youtube", new YouTubeToolHandler())
            .register("exit_Neuon", new ExitToolHandler(memory))
            .register("request_memories", new RequestMemoryToolHandler(memory))
            .register("read_file", new ReadFileToolHandler())
            .register("send_email", new EmailTool())
            .register("make_project_directory", new MakeDirectoryTool())
            .register("make_file", new MakeFileTool())
            .register("list_files", new ListFilesTool())
            .register("edit_file", new EditFileTool());

        ArrayList<String> toolNames = new ArrayList<>();
        toolNames.add("run_shell");
        toolNames.add("invoke_code_agent");
        toolNames.add("update_long_term_memory");
        toolNames.add("request_memories");
        toolNames.add("find_on_youtube");
        toolNames.add("read_file");
        toolNames.add("exit_Neuon");
        toolNames.add("list_files");
        toolNames.add("edit_file");

        this.orchestorTools = new ToolWareHouse().getNeededTools(toolNames).toJson();
    }

    public String getLLMResponse(String userPrompt) {
        try {
            String finalResponse = agentLoop.run(
                sysPrompt.getPrompt(),
                orchestorTools,
                userPrompt,
                memory.loadShortMemory()
            );
            memory.updateShortTermMemory(userPrompt, finalResponse);
            return finalResponse;
        } catch (Exception e) {
            return "[ERROR] " + e.toString();
        }
    }

    public void clearShortMemory() {
        memory.clearShortMemory();
        codeAgent.clearShortMemory();
    }
}
