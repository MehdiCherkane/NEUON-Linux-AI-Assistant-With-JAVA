package com.neuon.core;

import java.net.http.*;
import java.net.URI;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;

import com.google.gson.JsonObject;

public class LLMClient {

    private HttpResponse<String> response;
    private static final String API_KEY = System.getenv("GROQ_API_KEY");
    private static final String URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    public LLMResult ask(JsonObject body) {
        if (API_KEY == null || API_KEY.isBlank()) {
            return LLMResult.failure("GROQ_API_KEY environment variable not set");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status == 429) {
                String retryAfter = response.headers().firstValue("Retry-After").orElse("5");
                int delay = 5;
                try {
                    delay = Integer.parseInt(retryAfter);
                } catch (NumberFormatException ignored) {}
                try {
                    Thread.sleep(delay * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return LLMResult.failure(429, "Rate limited and interrupted during retry");
                }
                HttpRequest retryRequest = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
                response = httpClient.send(retryRequest, HttpResponse.BodyHandlers.ofString());
                status = response.statusCode();
                if (status == 429) {
                    return LLMResult.failure(429, "Rate limit exceeded after retry");
                }
            }

            if (status < 200 || status >= 300) {
                String errorMsg = "HTTP " + status + ": " + response.body();
                return LLMResult.failure(status, errorMsg);
            }

            return LLMResult.success(response.body(), status);
        } catch (SocketTimeoutException e) {
            return LLMResult.failure("Request timed out: " + e.getMessage());
        } catch (ConnectException e) {
            return LLMResult.failure("Connection failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return LLMResult.failure("Request interrupted");
        } catch (Exception e) {
            return LLMResult.failure("Network error: " + e.getMessage());
        }
    }

    public HttpResponse<String> getRawResponse(){
        return response;
    }
}
