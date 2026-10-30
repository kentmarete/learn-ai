package com.example.agent.agent;

import com.example.agent.llm.ChatTurn;
import com.example.agent.llm.LlmClient;
import com.example.agent.llm.ModelReply;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Convert a goal into short steps. The planner suggests; policy and code decide what may run. */
@Component
public class Planner {

    private static final Pattern STEP = Pattern.compile("^\\s*(?:\\d+[.)]|[-*])\\s+(.*\\S)");

    private final LlmClient llm;

    public Planner(LlmClient llm) {
        this.llm = llm;
    }

    // Treat model output as data: keep only numbered or bulleted lines, never execute them.
    public static List<String> parseSteps(ModelReply reply) {
        List<String> steps = new ArrayList<>();
        for (String line : reply.text().split("\\R")) {
            Matcher m = STEP.matcher(line);
            if (m.find() && steps.size() < 5) {
                steps.add(m.group(1));
            }
        }
        return steps;
    }

    public List<String> createPlan(AgentState state) {
        String prompt = """
                Goal: %s
                Return 3-5 short executable steps as a numbered list.
                Use tools only when required.
                """.formatted(state.query);
        state.plan = parseSteps(llm.ask(List.of(ChatTurn.Text.user(prompt))));
        return state.plan;
    }
}
