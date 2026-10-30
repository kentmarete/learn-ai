package com.example.agent.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.beta.AnthropicBeta;
import com.anthropic.models.beta.messages.BetaContentBlock;
import com.anthropic.models.beta.messages.BetaContentBlockParam;
import com.anthropic.models.beta.messages.BetaFallbacksParam;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaMessageParam;
import com.anthropic.models.beta.messages.BetaTextBlock;
import com.anthropic.models.beta.messages.BetaTool;
import com.anthropic.models.beta.messages.BetaToolResultBlockParam;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.example.agent.config.AgentProperties;
import com.example.agent.tools.AgentTool;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AnthropicLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmClient.class);

    private final AgentProperties props;
    private AnthropicClient client;

    public AnthropicLlmClient(AgentProperties props) {
        this.props = props;
    }

    // Created lazily so the app and tests start without credentials.
    private synchronized AnthropicClient client() {
        if (client == null) {
            client = AnthropicOkHttpClient.builder()
                    .fromEnv()
                    .timeout(Duration.ofSeconds(props.timeoutSeconds()))
                    .maxRetries(2)
                    .build();
        }
        return client;
    }

    @Override
    public ModelReply ask(List<ChatTurn> messages, List<AgentTool> tools, String system) {
        MessageCreateParams.Builder params = MessageCreateParams.builder()
                .model(props.model())
                .maxTokens(props.maxTokens())
                .messages(messages.stream().map(AnthropicLlmClient::toParam).toList())
                // If a safety classifier declines, the API re-runs the request on a recommended fallback model.
                .addBeta(AnthropicBeta.SERVER_SIDE_FALLBACK_2026_07_01)
                .fallbacks(BetaFallbacksParam.ofDefault());
        tools.forEach(tool -> params.addTool(toBetaTool(tool)));
        if (system != null) {
            params.system(system);
        }

        BetaMessage response = client().beta().messages().create(params.build());
        String stopReason = response.stopReason().map(r -> r.asString()).orElse("unknown");
        log.info("model={} stop_reason={} input_tokens={} output_tokens={}",
                response.model().asString(), stopReason,
                response.usage().inputTokens(), response.usage().outputTokens());

        String text = response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(BetaTextBlock::text)
                .collect(Collectors.joining()).strip();
        List<ToolCall> calls = response.content().stream()
                .filter(BetaContentBlock::isToolUse)
                .map(BetaContentBlock::asToolUse)
                .map(t -> new ToolCall(t.id(), t.name(), toMap(t._input())))
                .toList();
        return new ModelReply(stopReason, text, calls, response);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(JsonValue input) {
        return input.convert(Map.class);
    }

    private static BetaMessageParam toParam(ChatTurn turn) {
        return switch (turn) {
            case ChatTurn.Text t when t.role().equals("assistant") -> BetaMessageParam.ofAssistant(t.text());
            case ChatTurn.Text t -> BetaMessageParam.ofUser(t.text());
            // Send the assistant turn back unchanged, including its tool_use and thinking blocks.
            case ChatTurn.AssistantReply r -> ((BetaMessage) r.reply().raw()).toParam();
            // All results for one turn go back together in a single user message.
            case ChatTurn.ToolResults r -> BetaMessageParam.ofUserOfContentBlockParams(r.results().stream()
                    .map(res -> BetaContentBlockParam.ofToolResult(BetaToolResultBlockParam.builder()
                            .toolUseId(res.toolUseId())
                            .content(res.content())
                            .isError(res.isError())
                            .build()))
                    .toList());
        };
    }

    private static BetaTool toBetaTool(AgentTool tool) {
        BetaTool.InputSchema.Properties.Builder properties = BetaTool.InputSchema.Properties.builder();
        tool.properties().forEach((name, schema) -> properties.putAdditionalProperty(name, JsonValue.from(schema)));
        return BetaTool.builder()
                .name(tool.name())
                .description(tool.description())
                .inputSchema(BetaTool.InputSchema.builder()
                        .properties(properties.build())
                        .required(tool.required())
                        .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                        .build())
                .strict(true)
                .build();
    }
}
