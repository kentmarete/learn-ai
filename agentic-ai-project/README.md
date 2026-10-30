# Agentic AI Project: a small, production-style AI agent in Python

A minimal agent built from the 9-page "Agentic AI Project" notes: an observe → think → act loop on top of Claude, with allow-listed tools, safe execution, bounded memory, RAG over your own documents, a FastAPI endpoint, and deterministic tests.

## Project structure

```
agentic-ai-project/
├── src/agent_app/
│   ├── config.py      environment & limits
│   ├── llm_client.py  model requests (one gateway)
│   ├── state.py       shared agent state
│   ├── planner.py     creates next steps
│   ├── tools.py       safe external actions (allow-list + schemas)
│   ├── executor.py    runs approved tools (validation + timeout)
│   ├── memory.py      bounded conversation history
│   ├── rag.py         private knowledge search
│   ├── agent.py       observe-think-act loop
│   └── api.py         HTTP endpoints
├── data/knowledge/    your .md / .txt documents for RAG
├── tests/             deterministic checks (model is stubbed)
├── .env.example
├── requirements.txt
└── main.py
```

**Core flow:** User query → Retrieve context → Plan → Use tool → Observe → Final answer.

## 1. Set up

Python 3.10 or newer is required.

```bash
python -m venv .venv
source .venv/bin/activate        # Windows: .venv\Scripts\Activate.ps1
pip install -r requirements.txt
cp .env.example .env             # then add your key
export ANTHROPIC_API_KEY="..."
```

The model defaults to `claude-opus-5-5`; override it with `MODEL_NAME`. Other limits (`MAX_STEPS`, `TIMEOUT_S`, `TOOL_TIMEOUT_S`) are in `config.py`.

## 2. Run the tests

The tests stub the model, so they need no API key and cost nothing.

```bash
pytest -q        # or: make test
```

## 3. Run the API

```bash
python main.py   # or: make run
```

Then, in another terminal:

```bash
curl -X POST localhost:8000/ask -H 'Content-Type: application/json' \
  -d '{"query": "What is the weather in Delhi?"}'
# {"answer": "Delhi is currently 28°C."}

curl localhost:8000/health
```

Interactive docs are at http://localhost:8000/docs.

With Docker: `make docker-run` (reads `.env`).

## Extending it

- **Add a tool:** write the function in `tools.py`, add it to `TOOL_REGISTRY`, and add its JSON schema to `TOOL_SCHEMAS`. Nothing outside the registry can run.
- **Add knowledge:** drop `.md` or `.txt` files into `data/knowledge/`; they are indexed on first request.
- **Real RAG:** `HashingEmbedder` and `InMemoryVectorDB` in `rag.py` are dependency-free stand-ins. Replace them with a real embedding model and vector database; `ingest()` and `retrieve()` keep the same shape.
- **Weather is stubbed:** `get_weather` always returns 28°C. Swap in a real API call.

## Guardrails built in

- Max steps per run, tool allow-list, argument validation, per-tool timeout, a trace ID in the logs.
- Tool errors go back to the model as `is_error` results instead of crashing the loop.
- Refusals are handled, and the client opts into server-side fallback (`fallbacks: "default"`) so a declined request is retried on a recommended fallback model.

## Production checklist (not included)

Authentication, rate limiting, request IDs, a CORS policy, safe error messages, and human approval for risky tools.

## Evaluate more than accuracy

| Metric        | Question                       |
|---------------|--------------------------------|
| task success  | Was the goal completed?        |
| tool accuracy | Was the correct tool used?     |
| grounding     | Is the answer supported?       |
| safety        | Was the risky action blocked?  |
| latency       | How long did it take?          |
| cost          | How many tokens / tool calls?  |

**Build order:** deterministic tools → model gateway → bounded agent loop → tests and traces → memory and RAG. Start simple, measure every step, grant the minimum permission.
