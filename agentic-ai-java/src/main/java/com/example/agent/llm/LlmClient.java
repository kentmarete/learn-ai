package com.example.agent.llm;

import com.example.agent.tools.AgentTool;
import java.util.List;

/** One model gateway: a single place for retries, timeouts, logging and model changes. */
@FunctionalInterface
public interface LlmClient {

    ModelReply ask(List<ChatTurn> messages, List<AgentTool> tools, String system);

    default ModelReply ask(List<ChatTurn> messages) {
        return ask(messages, List.of(), null);
    }
}
