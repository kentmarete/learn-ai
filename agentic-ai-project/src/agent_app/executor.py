"""Validate before running. The model chooses a tool; trusted application code executes it."""
from concurrent.futures import ThreadPoolExecutor
from concurrent.futures import TimeoutError as FutureTimeout

from .config import settings
from .tools import TOOL_REGISTRY, TOOL_SCHEMAS

_SCHEMAS = {s["name"]: s["input_schema"] for s in TOOL_SCHEMAS}
_pool = ThreadPoolExecutor(max_workers=4)


def execute_tool(name: str, args: dict):
    if name not in TOOL_REGISTRY:
        raise ValueError("Tool not allowed")
    if not isinstance(args, dict):
        raise TypeError("Arguments must be an object")

    schema = _SCHEMAS[name]
    missing = [k for k in schema.get("required", []) if k not in args]
    unknown = [k for k in args if k not in schema.get("properties", {})]
    if missing or unknown:
        raise ValueError(f"Invalid arguments: missing={missing} unknown={unknown}")

    future = _pool.submit(TOOL_REGISTRY[name], **args)
    try:
        return future.result(timeout=settings.tool_timeout_s)
    except FutureTimeout:
        raise TimeoutError(f"Tool '{name}' timed out after {settings.tool_timeout_s}s")
