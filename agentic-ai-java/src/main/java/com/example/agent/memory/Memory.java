package com.example.agent.memory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Bounded session memory. Good memory is selective, scoped and disposable. */
public class Memory {

    public record Item(String role, String content) {
    }

    private final int limit;
    private final Deque<Item> items = new ArrayDeque<>();

    public Memory() {
        this(12);
    }

    public Memory(int limit) {
        this.limit = limit;
    }

    public synchronized void add(String role, String content) {
        items.addLast(new Item(role, content));
        while (items.size() > limit) {
            items.removeFirst();
        }
    }

    public synchronized List<Item> messages() {
        return List.copyOf(items);
    }

    public synchronized void clear() {
        items.clear();
    }
}
