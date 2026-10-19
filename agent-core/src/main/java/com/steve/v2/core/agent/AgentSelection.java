package com.steve.v2.core.agent;

import java.util.List;

/** UI selection without a hard-coded agent name. Empty means let the server resolve an unambiguous owner. */
public final class AgentSelection {
    private AgentSelection() {}
    public static String choose(String previous, List<String> visible) {
        String remembered = previous == null ? "" : previous.strip();
        for (String name : visible) if (name.equalsIgnoreCase(remembered)) return name;
        if (visible.size() == 1) return visible.getFirst();
        return visible.isEmpty() ? remembered : "";
    }
}
