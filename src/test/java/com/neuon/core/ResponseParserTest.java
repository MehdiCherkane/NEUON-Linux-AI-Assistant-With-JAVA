package com.neuon.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResponseParserTest {

    @Test
    void parsesNormalTextResponse() {
        String json = """
            {
                "id": "chatcmpl-123",
                "choices": [{
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": "Hello boss"
                    },
                    "finish_reason": "stop"
                }]
            }
            """;
        ResponseParser parser = new ResponseParser().parse(json);
        assertFalse(parser.isError());
        assertTrue(parser.isDone());
        assertFalse(parser.isToolCall());
        assertEquals("Hello boss", parser.getText());
        assertNotNull(parser.getRawMessage());
    }

    @Test
    void parsesToolCallResponse() {
        String json = """
            {
                "id": "chatcmpl-456",
                "choices": [{
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": null,
                        "tool_calls": [{
                            "id": "call_1",
                            "type": "function",
                            "function": {
                                "name": "run_shell",
                                "arguments": "{\\"command\\":\\"ls\\"}"
                            }
                        }]
                    },
                    "finish_reason": "tool_calls"
                }]
            }
            """;
        ResponseParser parser = new ResponseParser().parse(json);
        assertFalse(parser.isError());
        assertTrue(parser.isToolCall());
        assertFalse(parser.isDone());
        assertNotNull(parser.getToolCalls());
        assertEquals(1, parser.getToolCalls().size());
        assertEquals("call_1", ResponseParser.getToolCallId(parser.getToolCalls().get(0).getAsJsonObject()));
        assertEquals("run_shell", ResponseParser.getToolName(parser.getToolCalls().get(0).getAsJsonObject()));
    }

    @Test
    void parsesGroqErrorResponse() {
        String json = """
            {
                "error": {
                    "message": "Rate limit exceeded",
                    "type": "rate_limit_error",
                    "code": 429
                }
            }
            """;
        ResponseParser parser = new ResponseParser().parse(json);
        assertTrue(parser.isError());
        assertFalse(parser.isDone());
        assertNotNull(parser.getText());
        assertTrue(parser.getText().contains("Rate limit exceeded"));
    }

    @Test
    void handlesMalformedJson() {
        String malformed = "this is not json at all!!";
        ResponseParser parser = new ResponseParser().parse(malformed);
        assertTrue(parser.isError());
        assertNotNull(parser.getText());
    }

    @Test
    void handlesEmptyChoices() {
        String json = """
            {
                "id": "chatcmpl-789",
                "choices": []
            }
            """;
        ResponseParser parser = new ResponseParser().parse(json);
        assertTrue(parser.isError());
        assertTrue(parser.getText().contains("No choices"));
    }

    @Test
    void handlesNullContent() {
        String json = """
            {
                "id": "chatcmpl-101",
                "choices": [{
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": null
                    },
                    "finish_reason": "stop"
                }]
            }
            """;
        ResponseParser parser = new ResponseParser().parse(json);
        assertFalse(parser.isError());
        assertTrue(parser.isDone());
        assertNull(parser.getText());
    }

    @Test
    void extractsToolNameFromMultipleCalls() {
        String json = """
            {
                "id": "chatcmpl-111",
                "choices": [{
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": null,
                        "tool_calls": [{
                            "id": "call_1",
                            "type": "function",
                            "function": {
                                "name": "run_shell",
                                "arguments": "{\\"command\\":\\"ls\\"}"
                            }
                        }, {
                            "id": "call_2",
                            "type": "function",
                            "function": {
                                "name": "read_file",
                                "arguments": "{\\"file_path\\":\\"/tmp/test\\"}"
                            }
                        }]
                    },
                    "finish_reason": "tool_calls"
                }]
            }
            """;
        ResponseParser parser = new ResponseParser().parse(json);
        assertEquals(2, parser.getToolCalls().size());
        assertEquals("run_shell", ResponseParser.getToolName(parser.getToolCalls().get(0).getAsJsonObject()));
        assertEquals("read_file", ResponseParser.getToolName(parser.getToolCalls().get(1).getAsJsonObject()));
    }
}
