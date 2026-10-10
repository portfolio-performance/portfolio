"""Settings from the environment (N1). There is no other configuration source."""

import os
from dataclasses import dataclass

DEFAULT_API_URL = "http://127.0.0.1:5712"


@dataclass(frozen=True)
class Settings:
    api_url: str
    api_token: str | None

    @classmethod
    def from_env(cls) -> "Settings":
        url = os.environ.get("PP_API_URL", "").strip() or DEFAULT_API_URL
        token = os.environ.get("PP_API_TOKEN", "").strip() or None
        return cls(api_url=url.rstrip("/"), api_token=token)
