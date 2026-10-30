package com.example.agent.llm;

import java.util.List;

/** One entry in the conversation sent to the model. */
public sealed interface ChatTurn {

    record Text(String role, String text) implements ChatTurn {
        public static Text user(String text) {
            return new Text("user", text);
        }

        public static Text assistant(String text) {
            return new Text("assistant", text);
        }
    }

    record AssistantReply(ModelReply reply) implements ChatTurn {
    }

    record ToolResults(List<ToolResult> results) implements ChatTurn {
    }

    record ToolResult(String toolUseId, String content, boolean isError) {
    }
}
