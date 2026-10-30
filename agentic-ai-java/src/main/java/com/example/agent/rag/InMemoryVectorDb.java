package com.example.agent.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Dependency-free stand-in for a vector database (pgvector, Chroma, Qdrant...). */
public class InMemoryVectorDb {

    public record Match(String text, String source, double score) {
    }

    private record Row(double[] vector, Chunk chunk) {
    }

    private final List<Row> rows = new ArrayList<>();

    // Save both vectors and the original text + metadata.
    public synchronized void upsert(List<double[]> vectors, List<Chunk> chunks) {
        for (int i = 0; i < chunks.size(); i++) {
            rows.add(new Row(vectors.get(i), chunks.get(i)));
        }
    }

    public synchronized List<Match> search(double[] queryVector, int topK) {
        return rows.stream()
                .map(row -> new Match(row.chunk().text(), row.chunk().source(), dot(queryVector, row.vector())))
                .filter(m -> m.score() > 0)
                .sorted(Comparator.comparingDouble(Match::score).reversed())
                .limit(topK)
                .toList();
    }

    private static double dot(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }
}
