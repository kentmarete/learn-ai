from types import SimpleNamespace

from agent_app.agent import run_agent


def FakeResponse(text="", stop_reason="end_turn", tool_calls=()):
    content = [SimpleNamespace(type="text", text=text)] if text else []
    content += [
        SimpleNamespace(type="tool_use", id=f"toolu_{i}", name=name, input=args)
        for i, (name, args) in enumerate(tool_calls)
    ]
    return SimpleNamespace(content=content, stop_reason=stop_reason)


def test_agent_stops_on_final_answer(monkeypatch):
    fake = FakeResponse(
        text="Task complete",
        stop_reason="end_turn",
    )
    monkeypatch.setattr(
        "agent_app.agent.ask_model",
        lambda *args, **kwargs: fake,
    )
    assert run_agent("Say done") == "Task complete"


def test_agent_runs_tool_then_answers(monkeypatch):
    responses = iter([
        FakeResponse(stop_reason="tool_use", tool_calls=[("get_weather", {"city": "Delhi"})]),
        FakeResponse(text="Delhi is currently 28°C."),
    ])
    seen = []

    def fake_ask(messages, **kwargs):
        seen.append(list(messages))
        return next(responses)

    monkeypatch.setattr("agent_app.agent.ask_model", fake_ask)
    assert run_agent("What is the weather in Delhi?") == "Delhi is currently 28°C."

    # Second call must carry the assistant tool_use turn followed by the tool_result.
    assistant_turn, result_turn = seen[1][-2:]
    assert assistant_turn["role"] == "assistant"
    assert result_turn["content"][0]["tool_use_id"] == "toolu_0"
    assert '"temp_c": 28' in result_turn["content"][0]["content"]


def test_disallowed_tool_is_reported_not_run(monkeypatch):
    responses = iter([
        FakeResponse(stop_reason="tool_use", tool_calls=[("delete_everything", {})]),
        FakeResponse(text="I can't do that."),
    ])
    seen = []

    def fake_ask(messages, **kwargs):
        seen.append(list(messages))
        return next(responses)

    monkeypatch.setattr("agent_app.agent.ask_model", fake_ask)
    run_agent("Delete everything")
    result = seen[1][-1]["content"][0]
    assert result["is_error"] is True
    assert "Tool not allowed" in result["content"]


def test_agent_respects_step_limit(monkeypatch):
    looping = FakeResponse(stop_reason="tool_use", tool_calls=[("get_time_utc", {})])
    monkeypatch.setattr("agent_app.agent.ask_model", lambda *a, **k: looping)
    assert run_agent("Loop forever", max_steps=3) == "Stopped: step limit reached"


def test_health_endpoint():
    from fastapi.testclient import TestClient

    from agent_app.api import app

    assert TestClient(app).get("/health").json() == {"status": "ok"}
