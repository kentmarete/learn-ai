package com.example.agent.llm;

import java.util.List;

/**
 * Provider-neutral view of one model response. {@code raw} keeps the original SDK message so the
 * assistant turn (including thinking blocks) can be sent back unchanged.
 */
public record ModelReply(String stopReason, String text, List<ToolCall> toolCalls, Object raw) {

    public static ModelReply text(String text) {
        return new ModelReply("end_turn", text, List.of(), null);
    }

    public static ModelReply toolUse(ToolCall... calls) {
        return new ModelReply("tool_use", "", List.of(calls), null);
    }
}
