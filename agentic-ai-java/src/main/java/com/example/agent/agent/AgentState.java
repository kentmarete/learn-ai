package com.example.agent.agent;

import com.example.agent.llm.ChatTurn;
import java.util.ArrayList;
import java.util.List;

/** Shared agent state, so data flow between steps is explicit. */
public class AgentState {

    public final String query;
    public final List<ChatTurn> messages = new ArrayList<>();
    public List<String> plan = new ArrayList<>();
    public final List<Object> observations = new ArrayList<>();
    public int step = 0;
    public boolean done = false;

    public AgentState(String query) {
        this.query = query;
    }
}
