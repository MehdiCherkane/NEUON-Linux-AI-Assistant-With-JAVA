package com.neuon.tools;

import com.google.gson.JsonObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class YouTubeToolHandler implements ToolHandler{
        private Runner runner = new Runner();
    
        @Override
        public String execute(JsonObject parameters) {
            String query = parameters.get("search_query").getAsString();
            String url = "https://www.youtube.com/results?search_query=" 
                    + URLEncoder.encode(query, StandardCharsets.UTF_8);
            ProcessResult result = runner.execute("xdg-open '" + url.replace("'", "'\\''") + "'");
            return "Browser command finished with exit code: " + result.getExitCode();
        }
}
