package com.example.agent.agent;

import com.example.agent.config.AgentProperties;
import com.example.agent.llm.ChatTurn;
import com.example.agent.llm.LlmClient;
import com.example.agent.llm.ModelReply;
import com.example.agent.llm.ToolCall;
import com.example.agent.memory.Memory;
import com.example.agent.rag.Chunk;
import com.example.agent.rag.RagService;
import com.example.agent.tools.ToolExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** The agent loop: observe, decide, act. An agent is a controlled loop, not an unlimited process. */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    static final String SYSTEM_PROMPT = """
            You are a helpful assistant that can call tools.
            Use only the supplied context for factual claims about the knowledge base, and cite its source.
            If the context does not contain the answer and no tool can provide it, say so instead of guessing.""";

    private final LlmClient llm;
    private final ToolExecutor executor;
    private final RagService rag;
    private final AgentProperties props;

    public AgentService(LlmClient llm, ToolExecutor executor, RagService rag, AgentProperties props) {
        this.llm = llm;
        this.executor = executor;
        this.rag = rag;
        this.props = props;
    }

    static String buildSystem(List<Chunk> context) {
        if (context.isEmpty()) {
            return SYSTEM_PROMPT + "\n\n<context>No relevant documents found.</context>";
        }
        String docs = context.stream()
                .map(c -> "<doc source=\"" + c.source() + "\">\n" + c.text() + "\n</doc>")
                .collect(Collectors.joining("\n"));
        return SYSTEM_PROMPT + "\n\n<context>\n" + docs + "\n</context>";
    }

    public String run(String query) {
        return run(query, props.maxSteps(), null);
    }

    public String run(String query, int maxSteps, Memory memory) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        AgentState state = new AgentState(query);

        List<Memory.Item> history = memory != null ? new ArrayList<>(memory.messages()) : new ArrayList<>();
        while (!history.isEmpty() && !history.getFirst().role().equals("user")) {
            history.removeFirst(); // a conversation must start with a user turn
        }
        history.forEach(item -> state.messages.add(new ChatTurn.Text(item.role(), item.content())));
        state.messages.add(ChatTurn.Text.user(query));

        // 1. Observe: load relevant context once per query.
        String system = buildSystem(rag.retrieve(query));

        String answer = "Stopped: step limit reached";
        for (int step = 0; step < maxSteps; step++) {
            state.step = step + 1;
            // 2. Think: ask the model for the next action (tool call or final answer).
            ModelReply reply = llm.ask(state.messages, executor.tools(), system);
            log.info("trace={} step={} stop_reason={}", traceId, state.step, reply.stopReason());

            if ("refusal".equals(reply.stopReason())) {
                answer = "Sorry, I can't help with that request.";
                break;
            }
            if ("tool_use".equals(reply.stopReason()) && !reply.toolCalls().isEmpty()) {
                // 3. Act + 4. Observe: validate and run tools, feed results back.
                state.messages.add(new ChatTurn.AssistantReply(reply));
                state.messages.add(new ChatTurn.ToolResults(runTools(reply.toolCalls(), state)));
                continue;
            }
            // 5. Stop on final answer.
            answer = reply.text();
            break;
        }

        state.done = true;
        if (memory != null) {
            memory.add("user", query);
            memory.add("assistant", answer);
        }
        return answer;
    }

    private List<ChatTurn.ToolResult> runTools(List<ToolCall> calls, AgentState state) {
        List<ChatTurn.ToolResult> results = new ArrayList<>();
        for (ToolCall call : calls) {
            try {
                Object output = executor.execute(call.name(), call.input());
                results.add(new ChatTurn.ToolResult(call.id(), toJson(output), false));
                state.observations.add(Map.of("tool", call.name(), "input", call.input(), "output", output));
            } catch (Exception e) { // report the failure to the model instead of crashing the loop
                results.add(new ChatTurn.ToolResult(call.id(), String.valueOf(e.getMessage()), true));
                state.observations.add(Map.of("tool", call.name(), "error", String.valueOf(e.getMessage())));
            }
        }
        return results;
    }

    private static String toJson(Object value) throws JsonProcessingException {
        return JSON.writeValueAsString(value);
    }
}
