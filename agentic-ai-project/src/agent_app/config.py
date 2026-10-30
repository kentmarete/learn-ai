"""Environment-driven settings. Keep secrets in environment variables, not source code."""
import os
from dataclasses import dataclass
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]


@dataclass
class Settings:
    api_key: str = os.getenv("ANTHROPIC_API_KEY", "")
    model: str = os.getenv("MODEL_NAME", "claude-opus-5-5")
    max_steps: int = int(os.getenv("MAX_STEPS", "6"))
    max_tokens: int = int(os.getenv("MAX_TOKENS", "16000"))
    timeout_s: int = int(os.getenv("TIMEOUT_S", "30"))
    tool_timeout_s: int = int(os.getenv("TOOL_TIMEOUT_S", "10"))
    knowledge_dir: Path = Path(os.getenv("KNOWLEDGE_DIR", PROJECT_ROOT / "data" / "knowledge"))


settings = Settings()
