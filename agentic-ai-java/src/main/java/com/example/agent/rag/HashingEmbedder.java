package com.example.agent.rag;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dependency-free teaching stand-in: bag-of-words hashed into a fixed-size, L2-normalised vector.
 * Swap for a real embedding model in production.
 */
public class HashingEmbedder {

    private static final Pattern TOKEN = Pattern.compile("[a-z0-9]+");

    private final int dim;

    public HashingEmbedder() {
        this(512);
    }

    public HashingEmbedder(int dim) {
        this.dim = dim;
    }

    private double[] vector(String text) {
        double[] vec = new double[dim];
        Matcher m = TOKEN.matcher(text.toLowerCase(Locale.ROOT));
        while (m.find()) {
            vec[Math.floorMod(m.group().hashCode(), dim)] += 1.0;
        }
        double norm = 0;
        for (double v : vec) {
            norm += v * v;
        }
        norm = norm == 0 ? 1 : Math.sqrt(norm);
        for (int i = 0; i < dim; i++) {
            vec[i] /= norm;
        }
        return vec;
    }

    public List<double[]> embed(List<Chunk> chunks) {
        return chunks.stream().map(c -> vector(c.text())).toList();
    }

    public double[] embedQuery(String query) {
        return vector(query);
    }
}
