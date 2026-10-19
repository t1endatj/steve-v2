package com.steve.v2.core.planning;

import com.google.gson.*;
import com.steve.v2.core.task.*;
import java.util.*;

public final class PlanParser {
    private PlanParser() {}
    public static ActionPlan parse(String text) {
        if (text == null || text.length() > 131072) throw new IllegalArgumentException("Invalid plan response");
        String json = text.strip();
        if (json.startsWith("```json") && json.endsWith("```")) json = json.substring(7, json.length() - 3).strip();
        else if (json.startsWith("```") && json.endsWith("```")) json = json.substring(3, json.length() - 3).strip();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            List<Task> tasks = new ArrayList<>();
            for (JsonElement entry : root.getAsJsonArray("tasks")) {
                JsonObject item = entry.getAsJsonObject();
                Map<String, Object> params = new HashMap<>();
                for (var field : item.getAsJsonObject("parameters").entrySet()) params.put(field.getKey(), value(field.getValue()));
                tasks.add(new Task(item.get("action").getAsString(), params));
            }
            return new ActionPlan(root.get("plan").getAsString(), tasks);
        } catch (Exception e) {
            // Do not expose the entire remote response or silently drop malformed tasks.
            throw new IllegalArgumentException("Invalid AI plan: " + e.getMessage(), e);
        }
    }
    private static Object value(JsonElement e) {
        if (e.isJsonArray()) {
            List<Object> values = new ArrayList<>();
            for (var item : e.getAsJsonArray()) values.add(value(item));
            return List.copyOf(values);
        }
        if (!e.isJsonPrimitive()) throw new IllegalArgumentException("Parameters must be primitive values or arrays");
        JsonPrimitive p = e.getAsJsonPrimitive();
        if (p.isNumber()) return p.getAsBigDecimal();
        if (p.isBoolean()) return p.getAsBoolean();
        return p.getAsString();
    }
}
