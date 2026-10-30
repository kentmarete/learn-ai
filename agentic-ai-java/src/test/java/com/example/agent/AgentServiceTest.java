package com.example.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.agent.agent.AgentService;
import com.example.agent.llm.ChatTurn;
import com.example.agent.llm.LlmClient;
import com.example.agent.llm.ModelReply;
import com.example.agent.llm.ToolCall;
import com.example.agent.rag.RagService;
import com.example.agent.tools.TimeTool;
import com.example.agent.tools.ToolExecutor;
import com.example.agent.tools.WeatherTool;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The model is stubbed, so these tests are deterministic and need no API key. */
class AgentServiceTest {

    final ToolExecutor executor = new ToolExecutor(List.of(new WeatherTool(), new TimeTool()), ToolsTest.PROPS);
    final RagService rag = new RagService(ToolsTest.PROPS);
    final List<List<ChatTurn>> seen = new ArrayList<>();

    AgentService agentReplying(ModelReply... replies) {
        Iterator<ModelReply> it = List.of(replies).iterator();
        LlmClient fake = (messages, tools, system) -> {
            seen.add(List.copyOf(messages));
            return it.next();
        };
        return new AgentService(fake, executor, rag, ToolsTest.PROPS);
    }

    @Test
    void stopsOnFinalAnswer() {
        assertThat(agentReplying(ModelReply.text("Task complete")).run("Say done")).isEqualTo("Task complete");
    }

    @Test
    void runsToolThenAnswers() {
        AgentService agent = agentReplying(
                ModelReply.toolUse(new ToolCall("toolu_0", "get_weather", Map.of("city", "Nairobi"))),
                ModelReply.text("Nairobi is currently 28°C."));

        assertThat(agent.run("What is the weather in Nairobi?")).isEqualTo("Nairobi is currently 28°C.");

        // Second call must carry the assistant tool_use turn followed by the tool_result.
        List<ChatTurn> second = seen.get(1);
        assertThat(second.get(second.size() - 2)).isInstanceOf(ChatTurn.AssistantReply.class);
        ChatTurn.ToolResult result = ((ChatTurn.ToolResults) second.getLast()).results().getFirst();
        assertThat(result.toolUseId()).isEqualTo("toolu_0");
        assertThat(result.content()).contains("\"temp_c\":28");
        assertThat(result.isError()).isFalse();
    }

    @Test
    void disallowedToolIsReportedNotRun() {
        agentReplying(
                ModelReply.toolUse(new ToolCall("toolu_0", "delete_everything", Map.of())),
                ModelReply.text("I can't do that.")).run("Delete everything");

        ChatTurn.ToolResult result = ((ChatTurn.ToolResults) seen.get(1).getLast()).results().getFirst();
        assertThat(result.isError()).isTrue();
        assertThat(result.content()).contains("Tool not allowed");
    }

    @Test
    void respectsStepLimit() {
        ModelReply looping = ModelReply.toolUse(new ToolCall("toolu_0", "get_time_utc", Map.of()));
        AgentService agent = agentReplying(looping, looping, looping);
        assertThat(agent.run("Loop forever", 3, null)).isEqualTo("Stopped: step limit reached");
    }

    @Test
    void handlesRefusal() {
        ModelReply refusal = new ModelReply("refusal", "", List.of(), null);
        assertThat(agentReplying(refusal).run("Something disallowed")).startsWith("Sorry");
    }
}
