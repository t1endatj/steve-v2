package com.steve.v2.core.memory;

import java.util.ArrayDeque;
import java.util.List;

/** Bounded history. Persistence is provided by the host, e.g. Minecraft NBT. */
public final class AgentMemory {
    private final ArrayDeque<String> history = new ArrayDeque<>();
    public void record(String entry) {
        history.addLast(entry.length() > 512 ? entry.substring(0, 512) : entry);
        while (history.size() > 20) history.removeFirst();
    }
    public List<String> history() { return List.copyOf(history); }
}
