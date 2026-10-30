"""One model gateway: a single place for retries, timeouts, logging and model changes."""
import logging

from anthropic import Anthropic

from .config import settings

log = logging.getLogger(__name__)

_client: Anthropic | None = None


def get_client() -> Anthropic:
    # Created lazily so tests and imports work without credentials.
    global _client
    if _client is None:
        _client = Anthropic(
            api_key=settings.api_key or None,  # None lets the SDK resolve other credential sources
            timeout=settings.timeout_s,
            max_retries=2,
        )
    return _client


def ask_model(messages, tools=None, system=None):
    params = {
        "model": settings.model,
        "max_tokens": settings.max_tokens,
        "messages": messages,
        "tools": tools or [],
        # If a safety classifier declines, the API re-runs the request on a recommended fallback model.
        "betas": ["server-side-fallback-2026-07-01"],
        "fallbacks": "default",
    }
    if system:
        params["system"] = system
    response = get_client().beta.messages.create(**params)
    log.info(
        "model=%s stop_reason=%s input_tokens=%s output_tokens=%s",
        response.model, response.stop_reason,
        response.usage.input_tokens, response.usage.output_tokens,
    )
    return response
