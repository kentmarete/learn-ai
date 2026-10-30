"""Define the allowed actions. Only tools in the registry can ever run."""
from datetime import datetime, timezone
from typing import Callable


def get_weather(city: str) -> dict:
    # Stubbed for the demo - swap in a real weather API call here.
    return {"city": city, "temp_c": 28}


def get_time_utc() -> dict:
    return {"utc_time": datetime.now(timezone.utc).isoformat(timespec="seconds")}


TOOL_REGISTRY: dict[str, Callable] = {
    "get_weather": get_weather,
    "get_time_utc": get_time_utc,
}

TOOL_SCHEMAS = [
    {
        "name": "get_weather",
        "description": "Get the current weather for a city. Returns the temperature in Celsius.",
        "input_schema": {
            "type": "object",
            "properties": {"city": {"type": "string", "description": "City name, e.g. Nairobi"}},
            "required": ["city"],
            "additionalProperties": False,
        },
        "strict": True,
    },
    {
        "name": "get_time_utc",
        "description": "Get the current date and time in UTC.",
        "input_schema": {
            "type": "object",
            "properties": {},
            "additionalProperties": False,
        },
        "strict": True,
    },
]
