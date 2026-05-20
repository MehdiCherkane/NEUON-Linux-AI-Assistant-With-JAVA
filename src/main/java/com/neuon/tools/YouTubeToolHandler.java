package com.neuon.tools;

import com.google.gson.JsonObject;

public class YouTubeToolHandler implements ToolHandler{
        private Runner runner = new Runner();
    
        @Override
        public String execute(JsonObject parameters) {
            String query = parameters.get("search_query").getAsString();
            String url = "https://www.youtube.com/results?search_query=" 
                    + query.replace(" ", "+");
            ProcessResult result = runner.execute("brave \"" + url + "\"");
            return "Browser command finished with exit code: " + result.getExitCode();
        }
}
