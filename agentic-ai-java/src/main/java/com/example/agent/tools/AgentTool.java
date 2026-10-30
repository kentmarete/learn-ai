package com.example.agent.tools;

import java.util.List;
import java.util.Map;

/** An allowed action. Only beans implementing this interface can ever run. */
public interface AgentTool {

    String name();

    String description();

    /** JSON-schema property definitions, keyed by argument name. */
    Map<String, Object> properties();

    List<String> required();

    Object run(Map<String, Object> args);
}
