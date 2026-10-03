"""Mapping of RFC 9457 problem responses to MCP tool errors (N7)."""

from typing import Any

from fastmcp.exceptions import ToolError

PAIRING_HINT = (
    "Hint: create a token in Portfolio Performance under Preferences → REST API → "
    '"Add client" and pass it to the adapter in the PP_API_TOKEN environment variable.'
)


def short_type(problem_type: str | None, status: int) -> str:
    """`https://…/problems/validation` → `validation`."""
    if not problem_type:
        return f"http-{status}"
    return str(problem_type).rstrip("/").rsplit("/", 1)[-1]


def format_problem(status: int, problem: dict[str, Any]) -> str:
    """`{type}: {title} — {detail}` plus one line `field (code): message` per error."""
    kind = short_type(problem.get("type"), status)
    title = problem.get("title") or f"HTTP {status}"
    text = f"{kind}: {title}"
    if problem.get("detail"):
        text += f" — {problem['detail']}"
    for err in problem.get("errors") or []:
        if isinstance(err, dict):
            text += f"\n{err.get('field', '?')} ({err.get('code', '?')}): {err.get('message', '')}"
    if status == 401:
        text += "\n" + PAIRING_HINT
    return text


class PPApiError(ToolError):
    """An error answer of the PP REST API. Its message is the tool error text."""

    def __init__(self, status: int, problem: dict[str, Any]):
        self.status = status
        self.problem = problem
        self.type = short_type(problem.get("type"), status)
        self.title = problem.get("title")
        self.detail = problem.get("detail")
        self.errors = problem.get("errors") or []
        super().__init__(format_problem(status, problem))
