package com.neuon.core;

public record LLMResult(boolean ok, String body, int statusCode, String error) {
    public static LLMResult success(String body, int statusCode) {
        return new LLMResult(true, body, statusCode, null);
    }

    public static LLMResult failure(int statusCode, String error) {
        return new LLMResult(false, null, statusCode, error);
    }

    public static LLMResult failure(String error) {
        return new LLMResult(false, null, 0, error);
    }
}
