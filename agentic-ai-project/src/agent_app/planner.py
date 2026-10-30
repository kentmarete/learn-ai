"""Convert a goal into short steps. The planner suggests; policy and code decide what may run."""
import re

from .llm_client import ask_model
from .state import AgentState


def get_text(response) -> str:
    return "".join(b.text for b in response.content if b.type == "text").strip()


def parse_steps(response) -> list[str]:
    # Treat model output as data: keep only numbered or bulleted lines, never execute them.
    steps = []
    for line in get_text(response).splitlines():
        match = re.match(r"^\s*(?:\d+[.)]|[-*])\s+(.*\S)", line)
        if match:
            steps.append(match.group(1))
    return steps[:5]


def create_plan(state: AgentState) -> list[str]:
    prompt = f"""
    Goal: {state.query}
    Return 3-5 short executable steps as a numbered list.
    Use tools only when required.
    """
    response = ask_model([{"role": "user", "content": prompt}])
    state.plan = parse_steps(response)
    return state.plan
