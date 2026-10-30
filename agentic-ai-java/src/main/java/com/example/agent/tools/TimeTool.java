package com.example.agent.tools;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TimeTool implements AgentTool {

    @Override
    public String name() {
        return "get_time_utc";
    }

    @Override
    public String description() {
        return "Get the current date and time in UTC.";
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of();
    }

    @Override
    public List<String> required() {
        return List.of();
    }

    @Override
    public Object run(Map<String, Object> args) {
        return Map.of("utc_time", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
    }
}
