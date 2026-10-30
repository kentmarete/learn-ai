import pytest

from agent_app.executor import execute_tool
from agent_app.memory import Memory
from agent_app.rag import HashingEmbedder, InMemoryVectorDB, ingest, retrieve
from agent_app.tools import get_weather


def test_weather_tool():
    result = get_weather("Delhi")
    assert result["city"] == "Delhi"
    assert "temp_c" in result


def test_executor_runs_allowed_tool():
    assert execute_tool("get_weather", {"city": "Nairobi"})["city"] == "Nairobi"


def test_executor_blocks_unknown_tool():
    with pytest.raises(ValueError, match="Tool not allowed"):
        execute_tool("delete_everything", {})


def test_executor_rejects_bad_arguments():
    with pytest.raises(TypeError):
        execute_tool("get_weather", "Delhi")
    with pytest.raises(ValueError, match="missing"):
        execute_tool("get_weather", {})
    with pytest.raises(ValueError, match="unknown"):
        execute_tool("get_weather", {"city": "Delhi", "shell": "rm -rf /"})


def test_memory_is_bounded():
    memory = Memory(limit=2)
    for i in range(5):
        memory.add("user", f"msg {i}")
    assert [m["content"] for m in memory.messages()] == ["msg 3", "msg 4"]


def test_retrieve_finds_relevant_chunk():
    embedder, db = HashingEmbedder(), InMemoryVectorDB()
    ingest(
        [
            {"text": "Mangoes are sweet tropical fruit.", "source": "fruit.md"},
            {"text": "Vector databases store embeddings for similarity search.", "source": "rag.md"},
        ],
        embedder,
        db,
    )
    results = retrieve("how do vector databases work", embedder, db, k=1)
    assert results[0]["source"] == "rag.md"
