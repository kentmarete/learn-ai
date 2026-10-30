package com.example.agent.tools;

import com.example.agent.config.AgentProperties;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Validate before running. The model chooses a tool; trusted application code executes it. */
@Component
public class ToolExecutor {

    private final Map<String, AgentTool> registry;
    private final AgentProperties props;
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

    public ToolExecutor(List<AgentTool> tools, AgentProperties props) {
        this.registry = tools.stream().collect(Collectors.toMap(AgentTool::name, Function.identity()));
        this.props = props;
    }

    public List<AgentTool> tools() {
        return List.copyOf(registry.values());
    }

    public Object execute(String name, Object args) throws Exception {
        AgentTool tool = registry.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("Tool not allowed");
        }
        if (!(args instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Arguments must be an object");
        }

        List<String> missing = tool.required().stream().filter(k -> !map.containsKey(k)).toList();
        List<?> unknown = map.keySet().stream().filter(k -> !tool.properties().containsKey(k)).toList();
        if (!missing.isEmpty() || !unknown.isEmpty()) {
            throw new IllegalArgumentException("Invalid arguments: missing=" + missing + " unknown=" + unknown);
        }

        @SuppressWarnings("unchecked")
        Future<Object> future = pool.submit(() -> tool.run((Map<String, Object>) map));
        try {
            return future.get(props.toolTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Tool '" + name + "' timed out after " + props.toolTimeoutSeconds() + "s");
        } catch (ExecutionException e) {
            throw e.getCause() instanceof Exception cause ? cause : e;
        }
    }
}
