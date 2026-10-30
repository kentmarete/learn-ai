"""RAG over your own documents: ingest (split, embed, store) and retrieve (embed query, search).

The embedder and vector DB here are dependency-free teaching stand-ins. Swap HashingEmbedder for a
real embedding model and InMemoryVectorDB for a real vector database (pgvector, Chroma, Qdrant...).
"""
import hashlib
import math
import re
from dataclasses import dataclass
from pathlib import Path

from .config import settings


def split_text(doc: dict, chunk_size: int = 600, overlap: int = 80) -> list[dict]:
    # Sizes are in words here (a rough stand-in for tokens); overlap keeps context between chunks.
    words = doc["text"].split()
    step = max(chunk_size - overlap, 1)
    return [
        {"text": " ".join(words[i:i + chunk_size]), "source": doc["source"]}
        for i in range(0, max(len(words) - overlap, 1), step)
    ]


class HashingEmbedder:
    """Bag-of-words hashed into a fixed-size, L2-normalised vector."""

    def __init__(self, dim: int = 512):
        self.dim = dim

    def _vector(self, text: str) -> list[float]:
        vec = [0.0] * self.dim
        for token in re.findall(r"[a-z0-9]+", text.lower()):
            vec[int(hashlib.md5(token.encode()).hexdigest(), 16) % self.dim] += 1.0
        norm = math.sqrt(sum(v * v for v in vec)) or 1.0
        return [v / norm for v in vec]

    def embed(self, chunks: list[dict]) -> list[list[float]]:
        return [self._vector(c["text"]) for c in chunks]

    def embed_query(self, query: str) -> list[float]:
        return self._vector(query)


@dataclass
class Match:
    text: str
    source: str
    score: float


class InMemoryVectorDB:
    def __init__(self):
        self.rows: list[tuple[list[float], dict]] = []

    def upsert(self, vectors: list[list[float]], chunks: list[dict]):
        # Save both vectors and the original text + metadata.
        self.rows.extend(zip(vectors, chunks))

    def search(self, query_vector: list[float], top_k: int = 4) -> list[Match]:
        scored = [
            Match(chunk["text"], chunk["source"], sum(a * b for a, b in zip(query_vector, vec)))
            for vec, chunk in self.rows
        ]
        return [m for m in sorted(scored, key=lambda m: m.score, reverse=True)[:top_k] if m.score > 0]


def ingest(documents: list[dict], embedder, vector_db):
    chunks = []
    for doc in documents:
        chunks += split_text(
            doc,
            chunk_size=600,
            overlap=80,
        )
    vectors = embedder.embed(chunks)
    vector_db.upsert(vectors, chunks)


def load_documents(folder: Path) -> list[dict]:
    return [
        {"text": path.read_text(encoding="utf-8"), "source": path.name}
        for path in sorted(folder.glob("*"))
        if path.suffix in {".md", ".txt"}
    ]


_default: tuple[HashingEmbedder, InMemoryVectorDB] | None = None


def default_index() -> tuple[HashingEmbedder, InMemoryVectorDB]:
    global _default
    if _default is None:
        embedder, vector_db = HashingEmbedder(), InMemoryVectorDB()
        if settings.knowledge_dir.is_dir():
            ingest(load_documents(settings.knowledge_dir), embedder, vector_db)
        _default = (embedder, vector_db)
    return _default


def retrieve(query: str, embedder=None, vector_db=None, k: int = 4) -> list[dict]:
    if embedder is None or vector_db is None:
        embedder, vector_db = default_index()
    query_vector = embedder.embed_query(query)
    matches = vector_db.search(query_vector, top_k=k)
    return [
        {"text": m.text, "source": m.source}
        for m in matches
    ]
