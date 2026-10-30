# Agentic AI Project (Java / Spring Boot)

The Java version of `../agentic-ai-project`: an observe → think → act agent on Claude with allow-listed tools, safe execution, bounded memory, RAG over your own documents, a REST endpoint, and deterministic tests. Built with Spring Boot 3.5, Gradle and the official Anthropic Java SDK.

## Project structure

```
agentic-ai-java/
├── src/main/java/com/example/agent/
│   ├── AgentApplication.java     Spring Boot entry point
│   ├── config/AgentProperties    environment & limits (application.yml)
│   ├── llm/AnthropicLlmClient    model requests (one gateway, behind the LlmClient interface)
│   ├── agent/AgentState          shared agent state
│   ├── agent/Planner             creates next steps
│   ├── tools/                    safe external actions (each tool is an AgentTool bean)
│   ├── tools/ToolExecutor        runs approved tools (allow-list, validation, timeout)
│   ├── memory/Memory             bounded conversation history
│   ├── rag/                      private knowledge search
│   ├── agent/AgentService        observe-think-act loop
│   └── api/AgentController       HTTP endpoints
├── src/test/java/...             deterministic checks (model is stubbed)
├── data/knowledge/               your .md / .txt documents for RAG
├── build.gradle
└── gradlew                       Gradle wrapper (no local Gradle install needed)
```

| Python (`agentic-ai-project`) | Java |
|---|---|
| `config.py` | `AgentProperties` + `application.yml` |
| `llm_client.py` | `LlmClient` / `AnthropicLlmClient` |
| `state.py`, `planner.py` | `AgentState`, `Planner` |
| `tools.py`, `executor.py` | `AgentTool` beans, `ToolExecutor` |
| `memory.py`, `rag.py` | `Memory`, `RagService` (+ `HashingEmbedder`, `InMemoryVectorDb`) |
| `agent.py`, `api.py`, `main.py` | `AgentService`, `AgentController`, `AgentApplication` |

## 1. Set up

Java 21 or newer is required. Use the included Gradle wrapper (`./gradlew`); no separate Gradle install is needed.

```bash
export ANTHROPIC_API_KEY="..."
```

The model defaults to `claude-opus-5-5`; override it with `MODEL_NAME`. Other limits (`MAX_STEPS`, `MAX_TOKENS`, `TIMEOUT_S`, `TOOL_TIMEOUT_S`, `KNOWLEDGE_DIR`, `PORT`) are in `application.yml`.

## 2. Run the tests

The tests stub the model, so they need no API key and cost nothing.

```bash
./gradlew test        # or: make test
```

## 3. Run the API

```bash
./gradlew bootRun     # or: make run
```

Then, in another terminal:

```bash
curl -X POST localhost:8000/ask -H 'Content-Type: application/json' \
  -d '{"query": "What is the weather in Nairobi?"}'
# {"answer": "Nairobi is currently 28°C."}

curl localhost:8000/health
```

With Docker: `make docker-run` (reads `.env`; copy `.env.example` first).

## Extending it

- **Add a tool:** create a `@Component` that implements `AgentTool`. Spring picks it up and `ToolExecutor` registers it; nothing else can run.
- **Add knowledge:** drop `.md` or `.txt` files into `data/knowledge/`; they are indexed on the first request.
- **Real RAG:** `HashingEmbedder` and `InMemoryVectorDb` are dependency-free stand-ins. Replace them with a real embedding model and vector database.
- **Weather is stubbed:** `WeatherTool` always returns 28°C. Swap in a real API call.

## Guardrails built in

- Max steps per run, tool allow-list, argument validation, per-tool timeout, a trace ID in the logs.
- Tool errors go back to the model as `is_error` results instead of crashing the loop.
- Request validation on `/ask` (non-blank, max 4000 chars).
- Refusals are handled, and the client opts into server-side fallback (`fallbacks: "default"`) so a declined request is retried on a recommended fallback model.

## Production checklist (not included)

Authentication, rate limiting, request IDs, a CORS policy, safe error messages, and human approval for risky tools.
