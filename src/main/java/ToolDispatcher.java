import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.JsonObject;

public class ToolDispatcher {
    private HashMap<String, ToolHandler> handlers = new HashMap<>();

    public ToolDispatcher(){
        register("run_shell", new ShellTool());
        register("write_code", new CodeToolHandler());
        register("update_long_term_memory", new LongMemoryToolHandler());
        register("exit_Neuon", new ExitToolHandler());
        register("request_memories", new RequestMemoryToolHandler());
        register("read_file", new ReadFileToolHandler());
        register("send_email", new EmailTool());
    }

    public ToolDispatcher register(String toolName, ToolHandler handler){
        handlers.put(toolName, handler);
        return this;
    }

    public String dispatch(String toolName, JsonObject parameters){
        ToolHandler hanler = handlers.get(toolName);
        if (hanler != null) {
            return hanler.execute(parameters);
        }
        return "Error: this tool '" + toolName + "' is unkown"; 
    }

    public ArrayList<String> getAllToolsNames(){
        return new ArrayList<>(handlers.keySet());
    }
}
