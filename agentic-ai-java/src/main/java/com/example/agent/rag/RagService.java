package com.example.agent.rag;

import com.example.agent.config.AgentProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/** RAG over your own documents: ingest (split, embed, store) and retrieve (embed query, search). */
@Service
public class RagService {

    private final AgentProperties props;
    private HashingEmbedder embedder;
    private InMemoryVectorDb vectorDb;

    public RagService(AgentProperties props) {
        this.props = props;
    }

    // Sizes are in words here (a rough stand-in for tokens); overlap keeps context between chunks.
    public static List<Chunk> splitText(Chunk doc, int chunkSize, int overlap) {
        String[] words = doc.text().trim().split("\\s+");
        int step = Math.max(chunkSize - overlap, 1);
        List<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < Math.max(words.length - overlap, 1); i += step) {
            String text = String.join(" ", Arrays.copyOfRange(words, i, Math.min(i + chunkSize, words.length)));
            chunks.add(new Chunk(text, doc.source()));
        }
        return chunks;
    }

    public static void ingest(List<Chunk> documents, HashingEmbedder embedder, InMemoryVectorDb vectorDb) {
        List<Chunk> chunks = new ArrayList<>();
        for (Chunk doc : documents) {
            chunks.addAll(splitText(doc, 600, 80));
        }
        vectorDb.upsert(embedder.embed(chunks), chunks);
    }

    public static List<Chunk> retrieve(String query, HashingEmbedder embedder, InMemoryVectorDb vectorDb, int k) {
        double[] queryVector = embedder.embedQuery(query);
        return vectorDb.search(queryVector, k).stream()
                .map(m -> new Chunk(m.text(), m.source()))
                .toList();
    }

    public List<Chunk> retrieve(String query) {
        ensureIndexed();
        return retrieve(query, embedder, vectorDb, 4);
    }

    // Indexed lazily on first request; drop new files into the knowledge folder and restart.
    private synchronized void ensureIndexed() {
        if (vectorDb != null) {
            return;
        }
        embedder = new HashingEmbedder();
        vectorDb = new InMemoryVectorDb();
        Path dir = Path.of(props.knowledgeDir());
        if (Files.isDirectory(dir)) {
            ingest(loadDocuments(dir), embedder, vectorDb);
        }
    }

    static List<Chunk> loadDocuments(Path dir) {
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.toString().endsWith(".md") || p.toString().endsWith(".txt"))
                    .sorted()
                    .map(p -> {
                        try {
                            return new Chunk(Files.readString(p), p.getFileName().toString());
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
