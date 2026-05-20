package com.neuon.core;

import com.neuon.agent.*;
import java.net.http.*;
import java.net.URI;
import com.google.gson.JsonObject;

public class LLMClient {

    private HttpResponse<String> response;
    private PromptV2 systemPrompt = new PromptV2();
    private Memory memory = new Memory();

    private static final String API_KEY = System.getenv("GROQ_API_KEY");

    private static final String URL = "https://api.groq.com/openai/v1/chat/completions";

    public String ask(JsonObject body) throws Exception {
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + API_KEY)
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
        
        response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString());
        
        return response.body();
    }

    // This method is for debugging.
    public HttpResponse<String> getRawResponse(){
        return response;
    }
    
}