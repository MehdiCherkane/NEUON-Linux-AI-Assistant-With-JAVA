package com.neuon.tools;

import com.neuon.agent.*;
import java.util.ArrayList;
import java.util.HashMap;

public class ToolWareHouse {
    private ToolOptimzerModel optimizer;

    private HashMap<String, ToolDefinition> allTools = new HashMap<>();
    // I need to store all tools in in a HashMap.
    
    public ToolWareHouse() {

        // 1. run_shell
        registerTool("run_shell", new ToolDefinition("run_shell",
            "Execute a terminal shell command on the user's Linux system. " +
            "Use this for system tasks: installing packages, running scripts, navigating the filesystem, " +
            "checking processes, or any OS-level operation. " +
            "Do NOT use this to write or create files — use make_file for that.")
            .addParameter("command", "string", true));

        // 2. write_code
        registerTool("invoke_code_agent", new ToolDefinition("invoke_code_agent",
            "Delegate a coding task to the specialized Code Agent. " +
            "Use this when the user asks to build, create, write, or fix a program or script. " +
            "The Code Agent will autonomously write files, compile, run, and debug until the task is complete. " +
            "Provide a clear and detailed prompt describing exactly what needs to be built, " +
            "including language, functionality, and any specific requirements the user mentioned.")
            .addParameter("prompt", "string", true)
    );
            
        // 3. find_on_youtube
        registerTool("find_on_youtube", new ToolDefinition("find_on_youtube",
            "Search YouTube for a video. Use this when the user wants to watch, find, or listen to " +
            "something on YouTube (tutorials, music, videos). " +
            "Provide a concise and relevant search query.")
            .addParameter("search_query", "string", true));

        // 4. update_long_term_memory
        registerTool("update_long_term_memory", new ToolDefinition("update_long_term_memory",
            "Permanently save an important piece of information about the user to long-term memory. " +
            "Use this when the user explicitly shares personal information, preferences, or facts " +
            "that should be remembered across future sessions (e.g. name, job, habits, goals). " +
            "Do NOT use this for temporary context or things only relevant to the current conversation.")
            .addParameter("memory_category", "string", true)
            .addParameter("something_to_remember", "string", true));

        // 5. exit_Neuon
        registerTool("exit_Neuon", new ToolDefinition("exit_Neuon",
            "Terminate the current session and close the assistant. " +
            "Use this ONLY when the user explicitly says they want to exit, quit, close, or end the session. " +
            "Do NOT use this if the user is simply done with a task but hasn't asked to exit.")
            .addParameter("exit_message", "string", false));

        // 6. request_memories
        registerTool("request_memories", new ToolDefinition("request_memories",
            "Retrieve previously saved information from long-term memory. " +
            "Use this at the start of a conversation or when the user references something personal " +
            "that you might have stored before (e.g. their name, preferences, past context). ")
            .addParameter("requested_memories", "string", true));

        // 7. read_file
        registerTool("read_file", new ToolDefinition("read_file",
            "Read and return the full text content of a file from the filesystem. " +
            "Use this when the user asks you to open, read, review, or analyze a specific file. " +
            "Provide the absolute file path. Do NOT use this to execute files — use run_shell for that.")
            .addParameter("file_path", "string", true));
        // 8 - send emails
        registerTool("send_email", new ToolDefinition("send_email", "Sends an email to a specified recipient. " +
           "Use this tool when the user wants to send, write, or compose an email. " +
           "Required parameters: 'to' (recipient email address), " +
           "'subject' (email subject line), 'body' (email content).")
           .addParameter("To", "string", true)
           .addParameter("Subject", "string", true)
           .addParameter("Body", "string", true)
        );
        // 9 - make directories (For Code Agent)
        registerTool("make_project_directory", new ToolDefinition("make_project_directory",
            "Create a new directory in the workspace to house the project files. " +
            "Always call this before creating any files for a new project. " +
            "Provide a clean, lowercase, underscore-separated name that reflects the project " +
            "(e.g. 'snake_game', 'web_scraper'). The directory will be created inside workspace/. " +
            "Do NOT provide a full path, just the folder name.")
            .addParameter("dir_name", "string", true)
        );
        // 10 - make files.
        registerTool("make_file", new ToolDefinition("make_file",
            "Create a new source file inside the project directory and write its full content. " +
            "Use this to write any code file (e.g. 'Main.java', 'script.py', 'index.js'). " +
            "Provide a path relative to the workspace, including the project directory " +
            "(for example 'snake_game/main.py'). The tool rejects paths outside the workspace.")
            .addParameter("file_path", "string", true)
            .addParameter("file_content", "string", true)
        );

        optimizer = new ToolOptimzerModel(allTools.keySet());

    }

    // for the optimzer.
    public ToolRegistry getNeededTools(String userPrompt){

        ToolRegistry toolRegistry = new ToolRegistry();
        ArrayList<String> neededTools = optimizer.getNeededTools(userPrompt);
        if (neededTools == null) {
            return getAllTools();
        }
        for (String toolName : neededTools) {
            ToolDefinition tool = allTools.get(toolName);
            if (tool != null) {
                toolRegistry.register(tool);
            }
        }
        return toolRegistry;
    }

    // to expose the needed tools for difrent agents now (orchester, codeAgent).
    public ToolRegistry getNeededTools(ArrayList<String> neededToolsList){

        ToolRegistry toolRegistry = new ToolRegistry();
        ArrayList<String> neededTools = neededToolsList;
        for (String toolName : neededTools) {
            ToolDefinition tool = allTools.get(toolName);
            if (tool != null) {
                toolRegistry.register(tool);
            }
        }
        return toolRegistry;
    }
    

    private void registerTool(String toolName, ToolDefinition tool){
        allTools.put(toolName, tool);
    }

    public ToolRegistry getAllTools(){
        ToolRegistry toolRegistry = new ToolRegistry();
        for (ToolDefinition tool : allTools.values()) {
            toolRegistry.register(tool);
        }
        return toolRegistry;
    }

    public ArrayList<String> getAllToolsNames(){
        return new ArrayList<>(allTools.keySet());
    }
    
}
