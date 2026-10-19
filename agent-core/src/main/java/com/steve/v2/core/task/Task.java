package com.steve.v2.core.task;

import java.util.List;
import java.util.Map;

/** Immutable task shared by the planner and game adapters. */
public record Task(String action, Map<String, Object> parameters) {
    public Task {
        if (action == null || action.isBlank()) throw new IllegalArgumentException("Missing action");
        if (parameters == null) throw new IllegalArgumentException("Missing parameters");
        parameters = parameters.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
            Map.Entry::getKey, e -> e.getValue() instanceof List<?> list ? List.copyOf(list) : e.getValue()));
    }
    public String getAction() { return action; }
    public Object getParameter(String key) { return parameters.get(key); }
    public String getStringParameter(String key) { return getStringParameter(key, null); }
    public String getStringParameter(String key, String fallback) {
        Object value = parameters.get(key);
        return value instanceof String string ? string : fallback;
    }
    public int getIntParameter(String key, int fallback) {
        Object value = parameters.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }
}
