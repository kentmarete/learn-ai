"""HTTP entry point. Keep transport logic thin; the agent stays testable outside the API."""
from fastapi import FastAPI
from pydantic import BaseModel, Field

from .agent import run_agent

app = FastAPI(title="Agentic AI API")


class AskRequest(BaseModel):
    query: str = Field(min_length=1, max_length=4000)


class AskResponse(BaseModel):
    answer: str


@app.post("/ask", response_model=AskResponse)
def ask(request: AskRequest):
    answer = run_agent(request.query)
    return AskResponse(answer=answer)


@app.get("/health")
def health():
    return {"status": "ok"}
