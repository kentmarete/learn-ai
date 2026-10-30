package com.example.agent.tools;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WeatherTool implements AgentTool {

    @Override
    public String name() {
        return "get_weather";
    }

    @Override
    public String description() {
        return "Get the current weather for a city. Returns the temperature in Celsius.";
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("city", Map.of("type", "string", "description", "City name, e.g. Nairobi"));
    }

    @Override
    public List<String> required() {
        return List.of("city");
    }

    @Override
    public Object run(Map<String, Object> args) {
        // Stubbed for the demo - swap in a real weather API call here.
        return Map.of("city", args.get("city"), "temp_c", 28);
    }
}
