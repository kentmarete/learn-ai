"""The agent loop: observe, decide, act. An agent is a controlled loop, not an unlimited process."""
import json
import logging
import uuid

from .config import settings
from .executor import execute_tool
from .llm_client import ask_model
from .memory import Memory
from .planner import get_text
from .rag import retrieve
from .state import AgentState
from .tools import TOOL_SCHEMAS

log = logging.getLogger(__name__)

SYSTEM_PROMPT = """You are a helpful assistant that can call tools.
Use only the supplied context for factual claims about the knowledge base, and cite its source.
If the context does not contain the answer and no tool can provide it, say so instead of guessing."""


def build_system(context: list[dict]) -> str:
    if not context:
        return SYSTEM_PROMPT + "\n\n<context>No relevant documents found.</context>"
    docs = "\n".join(f'<doc source="{c["source"]}">\n{c["text"]}\n</doc>' for c in context)
    return f"{SYSTEM_PROMPT}\n\n<context>\n{docs}\n</context>"


def get_tool_calls(response) -> list:
    return [b for b in response.content if b.type == "tool_use"]


def tool_result_messages(response, calls, state: AgentState) -> list[dict]:
    # The assistant turn (with its tool_use blocks) must precede the matching tool_result blocks,
    # and all results for one turn go back together in a single user message.
    results = []
    for call in calls:
        try:
            output = execute_tool(call.name, call.input)
            results.append({"type": "tool_result", "tool_use_id": call.id, "content": json.dumps(output)})
            state.observations.append({"tool": call.name, "input": call.input, "output": output})
        except Exception as exc:  # report the failure to the model instead of crashing the loop
            results.append({"type": "tool_result", "tool_use_id": call.id, "content": str(exc), "is_error": True})
            state.observations.append({"tool": call.name, "input": call.input, "error": str(exc)})
    return [
        {"role": "assistant", "content": response.content},
        {"role": "user", "content": results},
    ]


def run_agent(query: str, max_steps: int | None = None, memory: Memory | None = None) -> str:
    max_steps = max_steps or settings.max_steps
    trace_id = uuid.uuid4().hex[:8]
    state = AgentState(query=query)

    history = memory.messages() if memory else []
    while history and history[0]["role"] != "user":  # a conversation must start with a user turn
        history = history[1:]
    state.messages = history + [{"role": "user", "content": query}]

    # 1. Observe: load relevant context once per query.
    system = build_system(retrieve(query))

    for step in range(max_steps):
        state.step = step + 1
        # 2. Think: ask the model for the next action (tool call or final answer).
        response = ask_model(state.messages, tools=TOOL_SCHEMAS, system=system)
        log.info("trace=%s step=%s stop_reason=%s", trace_id, state.step, response.stop_reason)

        if response.stop_reason == "refusal":
            answer = "Sorry, I can't help with that request."
            break

        calls = get_tool_calls(response)
        if response.stop_reason == "tool_use" and calls:
            # 3. Act + 4. Observe: validate and run tools, feed results back.
            state.messages += tool_result_messages(response, calls, state)
            continue

        # 5. Stop on final answer.
        answer = get_text(response)
        break
    else:
        answer = "Stopped: step limit reached"

    state.done = True
    if memory:
        memory.add("user", query)
        memory.add("assistant", answer)
    return answer
