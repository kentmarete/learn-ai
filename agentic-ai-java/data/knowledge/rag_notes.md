# Retrieval-Augmented Generation (RAG)

RAG gives the agent relevant evidence from your own documents.
Documents are split into overlapping chunks, embedded as vectors and stored in a vector database.
At query time the question is embedded, the most similar chunks are retrieved, and they are passed
to the model as context. The model should cite the source and say when evidence is missing.
RAG does not guarantee correctness; evaluate retrieval separately from generation.
