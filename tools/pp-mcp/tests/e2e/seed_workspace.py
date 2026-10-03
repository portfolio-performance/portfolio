"""Seed a Portfolio Performance workspace so the REST API is enabled for given files.

Writes the REST plugin's instance preferences (server on, port, per-file access) and a
client token into the workspace, so a PP launched with `-data <workspace>` accepts the
token without any interaction. Prints the token.

Usage: python seed_workspace.py --workspace DIR --port 5799 --file PATH[=ALIAS] ...
"""

import argparse
import base64
import hashlib
import json
import os
import secrets
import uuid
from datetime import datetime, timezone
from pathlib import Path

PLUGIN_ID = "name.abuchen.portfolio.rest"


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def java_absolute_path(path: str) -> str:
    # File#getAbsolutePath on Windows keeps backslashes and the drive letter as given
    p = os.path.abspath(path)
    if len(p) > 1 and p[1] == ":":
        p = p[0].upper() + p[1:]
    return p


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--workspace", required=True)
    parser.add_argument("--port", type=int, default=5712)
    parser.add_argument("--file", action="append", required=True, help="PATH or PATH=ALIAS")
    parser.add_argument("--token", default=None)
    args = parser.parse_args()

    ws = Path(args.workspace)
    settings = ws / ".metadata" / ".plugins" / "org.eclipse.core.runtime" / ".settings"
    state = ws / ".metadata" / ".plugins" / PLUGIN_ID
    settings.mkdir(parents=True, exist_ok=True)
    state.mkdir(parents=True, exist_ok=True)

    lines = ["eclipse.preferences.version=1", "enabled=true", f"port={args.port}"]
    files = []
    for spec in args.file:
        path, _, alias = spec.partition("=")
        abs_path = java_absolute_path(path)
        node = b64url(abs_path.encode("utf-8"))
        lines.append(f"files/{node}/uuid={uuid.uuid4()}")
        lines.append(f"files/{node}/enabled=true")
        if alias:
            lines.append(f"files/{node}/alias={alias}")
        files.append(abs_path)
    (settings / f"{PLUGIN_ID}.prefs").write_text("\n".join(lines) + "\n", encoding="utf-8")

    token = args.token or b64url(secrets.token_bytes(32))
    token_hash = b64url(hashlib.sha256(token.encode("utf-8")).digest())
    clients = {
        "clients": [
            {
                "id": str(uuid.uuid4()),
                "tokenHash": token_hash,
                "name": "e2e",
                "created": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
            }
        ]
    }
    (state / "api-clients.json").write_text(json.dumps(clients, indent=2), encoding="utf-8")

    print(json.dumps({"token": token, "port": args.port, "files": files}))


if __name__ == "__main__":
    main()
