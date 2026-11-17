"""Show which models each provider offers right now (free model names change).

Run:  python3 scripts/list_models.py [provider]
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from our_framework import llm  # noqa: E402
from our_framework.config import PROVIDERS  # noqa: E402

for name in sys.argv[1:] or PROVIDERS:
    try:
        models = llm.list_models(name)
        if name == "openrouter":
            models = [m for m in models if m.endswith(":free")]
        print(f"== {name} ({len(models)}) ==")
        print("\n".join(f"  {m}" for m in models))
    except Exception as e:  # noqa: BLE001
        print(f"== {name}: ERROR {e}")
