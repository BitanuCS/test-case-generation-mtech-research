"""Settings: free AI providers, model names, and folder paths.

All providers speak the same "OpenAI-compatible" chat API, so one client
(llm.py) works for all of them. Model names on free tiers change often --
run `python3 scripts/list_models.py` to see what is available and update
MODELS below.

Free limits we measured (Nov 2026):
  Gemini      ~20 requests/day per model; big context.
  Groq        1000 requests/day per model, but only 8000 tokens per MINUTE
              (prompt + max answer), so only small requests fit.
  OpenRouter  50 free requests/day in total.
"""
import os
from pathlib import Path

from dotenv import load_dotenv

PROJECT_ROOT = Path(__file__).resolve().parent.parent
load_dotenv(PROJECT_ROOT / ".env")

# Tools and Defects4J checkouts must live in a path without spaces.
TOOLS_DIR = Path(os.environ.get("TOOLS", "/Users/bitu/mtech-tools"))
WORK_DIR = TOOLS_DIR / "work"          # checked-out bugs go here
CACHE_DB = PROJECT_ROOT / "cache" / "llm_cache.sqlite"

PROVIDERS = {
    "gemini": {
        "base_url": "https://generativelanguage.googleapis.com/v1beta/openai",
        "key_env": "GEMINI_API_KEY",
    },
    "groq": {
        "base_url": "https://api.groq.com/openai/v1",
        "key_env": "GROQ_API_KEY",
        "max_tokens": 2500,
        "max_request_tokens": 7800,     # free tier: 8000 tokens per minute
    },
    "openrouter": {
        "base_url": "https://openrouter.ai/api/v1",
        "key_env": "OPENROUTER_API_KEY",
        "max_tokens": 16000,
    },
}

# ONE model does every big job (planning, writing, fixing tests, writing
# mutants), so all results come from the same model (fair comparison).
# Gemini 3.5 flash-lite allows ~500 free requests/day.
MODELS = {
    "main": [("gemini", "gemini-3.5-flash-lite")],
    # The oracle panel: three AIs from different families vote on expected
    # results. Each member is a short fallback list within ONE model family.
    "panel": [
        [("gemini", "gemini-3.5-flash-lite")],
        [("groq", "openai/gpt-oss-120b"), ("groq", "openai/gpt-oss-20b")],
        [("groq", "qwen/qwen3.8-27b"), ("groq", "openai/gpt-oss-20b")],
    ],
}

RESULTS_DIR = PROJECT_ROOT / "results"
