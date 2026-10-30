"""Bounded session memory. Good memory is selective, scoped and disposable."""
from collections import deque


class Memory:
    def __init__(self, limit: int = 12):
        self.items = deque(maxlen=limit)

    def add(self, role: str, content: str):
        self.items.append({
            "role": role,
            "content": content,
        })

    def messages(self) -> list[dict]:
        return list(self.items)

    def clear(self):
        self.items.clear()
