package com.neuon.agent;

import com.neuon.core.*;
import com.neuon.UI.FXInterface;
import com.neuon.tools.*;

import com.google.gson.JsonArray;

import java.util.ArrayList;

public class CodeAgent {

    private AgentLoop agentLoop;
    private Memory memory;
    private PromptCodeAgent sysPrompt;
    private ToolRunner toolRunner;
    private final JsonArray codeAgentTools;

    public CodeAgent(ToolRunner toolRunner, FXInterface fxInterface) {
        this.toolRunner = toolRunner;
        this.memory = new Memory();
        this.sysPrompt = new PromptCodeAgent();
        this.agentLoop = new AgentLoop(toolRunner, fxInterface);

        ArrayList<String> toolNames = new ArrayList<>();
        toolNames.add("make_project_directory");
        toolNames.add("make_file");
        toolNames.add("run_shell");
        toolNames.add("list_files");
        toolNames.add("read_file");
        toolNames.add("edit_file");

        this.codeAgentTools = new ToolWareHouse().getNeededTools(toolNames).toJson();
    }

    public String startCoding(String promptFromNeuon) {
        try {
            String finalResult = agentLoop.run(
                sysPrompt.getPrompt(),
                codeAgentTools,
                promptFromNeuon,
                memory.loadShortMemory()
            );
            memory.updateShortTermMemory(promptFromNeuon, finalResult);
            return finalResult;
        } catch (Exception e) {
            return "[ERROR] " + e.toString();
        }
    }

    public void clearShortMemory() {
        memory.clearShortMemory();
    }
}
