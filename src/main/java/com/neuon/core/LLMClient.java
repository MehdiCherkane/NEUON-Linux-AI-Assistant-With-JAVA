package com.neuon.core;

import java.net.http.*;
import java.net.URI;
import com.google.gson.JsonObject;

public class LLMClient {

    private HttpResponse<String> response;
    private static final String API_KEY = System.getenv("GROQ_API_KEY");
    private static final String URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public String ask(JsonObject body) throws Exception {
        if (API_KEY == null || API_KEY.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY environment variable not set");
        }
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + API_KEY)
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
        response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public HttpResponse<String> getRawResponse(){
        return response;
    }
}