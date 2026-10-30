package com.example.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Environment-driven settings and limits. Secrets stay in environment variables, not source code. */
@ConfigurationProperties(prefix = "agent")
public record AgentProperties(
        String model,
        int maxSteps,
        long maxTokens,
        int timeoutSeconds,
        int toolTimeoutSeconds,
        String knowledgeDir) {
}
