package com.example.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.agent.config.AgentProperties;
import com.example.agent.memory.Memory;
import com.example.agent.rag.Chunk;
import com.example.agent.rag.HashingEmbedder;
import com.example.agent.rag.InMemoryVectorDb;
import com.example.agent.rag.RagService;
import com.example.agent.tools.TimeTool;
import com.example.agent.tools.ToolExecutor;
import com.example.agent.tools.WeatherTool;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolsTest {

    static final AgentProperties PROPS = new AgentProperties("test-model", 6, 1000, 30, 5, "data/knowledge");

    final ToolExecutor executor = new ToolExecutor(List.of(new WeatherTool(), new TimeTool()), PROPS);

    @Test
    void weatherTool() {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) new WeatherTool().run(Map.of("city", "Nairobi"));
        assertThat(result.get("city")).isEqualTo("Nairobi");
        assertThat(result).containsKey("temp_c");
    }

    @Test
    void executorRunsAllowedTool() throws Exception {
        assertThat(executor.execute("get_weather", Map.of("city", "Nairobi")))
                .isEqualTo(Map.of("city", "Nairobi", "temp_c", 28));
    }

    @Test
    void executorBlocksUnknownTool() {
        assertThatThrownBy(() -> executor.execute("delete_everything", Map.of()))
                .hasMessage("Tool not allowed");
    }

    @Test
    void executorRejectsBadArguments() {
        assertThatThrownBy(() -> executor.execute("get_weather", "Nairobi"))
                .hasMessage("Arguments must be an object");
        assertThatThrownBy(() -> executor.execute("get_weather", Map.of()))
                .hasMessageContaining("missing=[city]");
        assertThatThrownBy(() -> executor.execute("get_weather", Map.of("city", "Nairobi", "shell", "rm -rf /")))
                .hasMessageContaining("unknown=[shell]");
    }

    @Test
    void memoryIsBounded() {
        Memory memory = new Memory(2);
        for (int i = 0; i < 5; i++) {
            memory.add("user", "msg " + i);
        }
        assertThat(memory.messages()).extracting(Memory.Item::content).containsExactly("msg 3", "msg 4");
    }

    @Test
    void retrieveFindsRelevantChunk() {
        HashingEmbedder embedder = new HashingEmbedder();
        InMemoryVectorDb db = new InMemoryVectorDb();
        RagService.ingest(List.of(
                new Chunk("Mangoes are sweet tropical fruit.", "fruit.md"),
                new Chunk("Vector databases store embeddings for similarity search.", "rag.md")), embedder, db);
        List<Chunk> results = RagService.retrieve("how do vector databases work", embedder, db, 1);
        assertThat(results.getFirst().source()).isEqualTo("rag.md");
    }
}
