// this class will be reponsible for: stroring all the tools we have a long with thier parameters.
// return to us the needed tools determned by ToolOptimizer.

import java.util.ArrayList;
import java.util.HashMap;

public class ToolWareHouse {
    private ToolOptimzerModel optimizer = new ToolOptimzerModel();

    private HashMap<String, ToolDefinition> allTools = new HashMap<>();
    // I need to store all tools in in a HashMap.
    
    public ToolWareHouse() {

        // 1. run_shell
        registerTool("run_shell", new ToolDefinition("run_shell",
            "Execute a terminal shell command on the user's Linux system. " +
            "Use this for system tasks: installing packages, running scripts, navigating the filesystem, " +
            "checking processes, or any OS-level operation. " +
            "Do NOT use this to write or create files — use write_code for that.")
            .addParameter("command", "string", true));

        // 2. write_code
        registerTool("write_code", new ToolDefinition("write_code",
            "Create a source code file and save it to the AI_CODE directory. " +
            "Use this when the user asks to create, generate, or save a code file. " +
            "Provide ONLY the filename with its extension (e.g. 'Main.java', 'script.py'), NOT a full path. " +
            "Provide the complete file content. Do NOT use run_shell to write files.")
            .addParameter("file_name", "string", true)
            .addParameter("file_content", "string", true));
            
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
        
        registerTool("send_email", new ToolDefinition("send_email", "Sends an email to a specified recipient. " +
           "Use this tool when the user wants to send, write, or compose an email. " +
           "Required parameters: 'to' (recipient email address), " +
           "'subject' (email subject line), 'body' (email content).")
           .addParameter("To", "string", true)
           .addParameter("Subject", "string", true)
           .addParameter("Body", "string", true)
        );

    }

    public ToolRegistry getNeededTools(String userPrompt){

        ToolRegistry toolRegistry = new ToolRegistry();
        ArrayList<String> neededTools = optimizer.getNeededTools(userPrompt);
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
    
}
