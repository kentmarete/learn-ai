package com.example.agent.api;

import com.example.agent.agent.AgentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** HTTP entry point. Keep transport logic thin; the agent stays testable outside the API. */
@RestController
public class AgentController {

    public record AskRequest(@NotBlank @Size(max = 4000) String query) {
    }

    public record AskResponse(String answer) {
    }

    private final AgentService agent;

    public AgentController(AgentService agent) {
        this.agent = agent;
    }

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return new AskResponse(agent.run(request.query()));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
