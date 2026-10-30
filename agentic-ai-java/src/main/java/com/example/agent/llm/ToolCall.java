package com.example.agent.llm;

import java.util.Map;

public record ToolCall(String id, String name, Map<String, Object> input) {
}
