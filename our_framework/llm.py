"""One small client for all free AI providers.

Every answer is saved in a local SQLite cache, keyed by the exact request.
Asking the same question twice costs nothing and does not use up the
free daily limit. This also makes experiments repeatable.

Models are always given as a list of (provider, model) pairs: the first one
that can answer is used. A model is skipped when:
  - its provider has no API key in .env,
  - the request is too big for its free tier (e.g. Groq: 8k tokens/minute),
  - its free daily quota ran out during this run.
"""
import hashlib
import json
import os
import sqlite3
import time

import requests

from .config import CACHE_DB, MODELS, PROVIDERS


class LLMError(RuntimeError):
    pass


class QuotaExhausted(LLMError):
    """Every model in the list is out of free quota for today."""


class _Cache:
    def __init__(self, path):
        path.parent.mkdir(parents=True, exist_ok=True)
        self.db = sqlite3.connect(path)
        self.db.execute(
            "CREATE TABLE IF NOT EXISTS answers ("
            " key TEXT PRIMARY KEY, provider TEXT, model TEXT,"
            " request TEXT, answer TEXT, created REAL)"
        )

    def get(self, key):
        row = self.db.execute("SELECT answer FROM answers WHERE key=?", (key,)).fetchone()
        return row[0] if row else None

    def put(self, key, provider, model, request, answer):
        self.db.execute(
            "INSERT OR REPLACE INTO answers VALUES (?,?,?,?,?,?)",
            (key, provider, model, request, answer, time.time()),
        )
        self.db.commit()


_cache = _Cache(CACHE_DB)

# Counts for the current run: {"fresh": n, "cached": n, "by_model": {...}}
stats = {"fresh": 0, "cached": 0, "by_model": {}, "tokens_in": 0, "tokens_out": 0}

# (provider, model) pairs whose free daily quota ran out during this run.
_exhausted = set()


# Extra cache tag: repeated runs set this so they get fresh AI answers
# instead of re-using the first run's cached ones.
RUN_TAG = ""


def reset_stats():
    stats.update(fresh=0, cached=0, by_model={}, tokens_in=0, tokens_out=0)


def estimate_tokens(messages):
    return sum(len(m["content"]) for m in messages) // 3 + 50


def chat(messages, models=None, temperature=0.0, max_retries=5, use_cache=True):
    """Send a chat request and return the answer text.

    messages: list of {"role": "system"|"user"|"assistant", "content": str}
    models:   list of (provider, model) to try in order; default MODELS["main"].
    """
    models = models or MODELS["main"]
    errors, quota_out = [], 0
    for provider, model in models:
        try:
            return _chat_one(messages, provider, model, temperature, max_retries, use_cache)
        except LLMError as e:
            errors.append(str(e))
            quota_out += "quota used up" in str(e) or "Missing" in str(e)
    if quota_out == len(models):
        raise QuotaExhausted("All models are out of free quota for today:\n" + "\n".join(errors))
    raise LLMError("All models failed:\n" + "\n".join(errors))


def _chat_one(messages, provider, model, temperature, max_retries, use_cache):
    cfg = PROVIDERS[provider]
    max_tokens = cfg.get("max_tokens", 32000)
    payload = {"model": model, "messages": messages, "temperature": temperature,
               "max_tokens": max_tokens}
    request_text = json.dumps({"provider": provider, **payload}, sort_keys=True)
    key = hashlib.sha256((request_text + RUN_TAG).encode()).hexdigest()

    if use_cache:
        cached = _cache.get(key)
        if cached is not None:
            stats["cached"] += 1
            return cached

    if (provider, model) in _exhausted:
        raise LLMError(f"{provider}/{model}: daily free quota used up")
    api_key = os.environ.get(cfg["key_env"])
    if not api_key:
        raise LLMError(f"Missing {cfg['key_env']} in .env")
    budget = cfg.get("max_request_tokens")
    if budget and estimate_tokens(messages) + max_tokens > budget:
        raise LLMError(f"{provider}/{model}: request too big for free tier")

    wait = 5
    for _attempt in range(max_retries):
        try:
            resp = requests.post(
                f"{cfg['base_url']}/chat/completions",
                headers={"Authorization": f"Bearer {api_key}"},
                json=payload,
                timeout=300,
            )
        except requests.RequestException as e:
            time.sleep(wait)
            wait = min(wait * 2, 120)
            last = str(e)
            continue
        last = resp.text[:300]
        # Daily quota used up: waiting won't help today, skip this model from now on.
        if resp.status_code == 429 and any(s in resp.text for s in ("PerDay", "per day", "free-models-per-day")):
            _exhausted.add((provider, model))
            raise LLMError(f"{provider}/{model}: daily free quota used up")
        # 429 = per-minute rate limit; 5xx = provider hiccup. Wait and retry.
        if resp.status_code == 429 or resp.status_code >= 500:
            retry_after = resp.headers.get("retry-after")
            try:
                delay = float(retry_after) + 1 if retry_after else wait
            except ValueError:
                delay = wait
            time.sleep(min(delay, 120))
            wait = min(wait * 2, 120)
            continue
        if resp.status_code != 200:
            raise LLMError(f"{provider}/{model} HTTP {resp.status_code}: {resp.text[:300]}")
        body = resp.json()
        usage = body.get("usage") or {}
        # token counts (for cost comparisons); counted even if the answer is unusable
        stats["tokens_in"] += int(usage.get("prompt_tokens") or 0)
        stats["tokens_out"] += int(usage.get("completion_tokens") or 0)
        choice = body["choices"][0]
        answer = choice["message"].get("content") or ""
        if choice.get("finish_reason") == "length" or not answer.strip():
            # Cut off or empty: don't cache it; let the caller fall back to another model.
            raise LLMError(f"{provider}/{model}: answer was cut off or empty")
        _cache.put(key, provider, model, request_text, answer)
        stats["fresh"] += 1
        tag = f"{provider}/{model}"
        stats["by_model"][tag] = stats["by_model"].get(tag, 0) + 1
        return answer

    raise LLMError(f"{provider}/{model}: gave up after {max_retries} retries ({last[:150]})")


def list_models(provider):
    """Return the model ids a provider currently offers."""
    cfg = PROVIDERS[provider]
    api_key = os.environ.get(cfg["key_env"])
    if not api_key:
        raise LLMError(f"Missing {cfg['key_env']} in .env")
    resp = requests.get(
        f"{cfg['base_url']}/models",
        headers={"Authorization": f"Bearer {api_key}"},
        timeout=60,
    )
    resp.raise_for_status()
    return sorted(m["id"] for m in resp.json()["data"])
