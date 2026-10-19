package com.steve.v2.core.task;

import java.util.List;
import java.util.Map;

/** Reject malformed or excessive tasks before any world mutation. */
public final class TaskSchema {
    private TaskSchema() {}
    public static void validate(Task task) {
        Map<String, Object> p = task.parameters();
        switch (task.action()) {
            case "pathfind" -> position(p);
            case "mine" -> { text(p, "block"); integer(p, "quantity", 1, 4096); }
            case "place" -> { text(p, "block"); position(p); }
            case "follow" -> text(p, "player");
            case "attack" -> text(p, "target");
            case "build" -> {
                text(p, "structure");
                if (p.containsKey("x") || p.containsKey("y") || p.containsKey("z")) position(p);
                if (!(p.get("blocks") instanceof List<?> blocks) || blocks.isEmpty() || blocks.size() > 16
                    || blocks.stream().anyMatch(b -> !(b instanceof String s) || s.isBlank()))
                    throw new IllegalArgumentException("build.blocks must contain 1..16 block IDs");
                if (!(p.get("dimensions") instanceof List<?> dims) || dims.size() != 3)
                    throw new IllegalArgumentException("build.dimensions must be [width, height, depth]");
                long volume = 1;
                for (Object d : dims) { volume *= number(d, "dimension", 1, 64); }
                if (volume > 65536) throw new IllegalArgumentException("Build exceeds 65,536 blocks");
            }
            default -> throw new IllegalArgumentException("Unsupported action: " + task.action());
        }
    }
    private static void position(Map<String, Object> p) {
        integer(p, "x", -29999984, 29999984);
        integer(p, "y", -64, 319);
        integer(p, "z", -29999984, 29999984);
    }
    private static void text(Map<String, Object> p, String key) {
        if (!(p.get(key) instanceof String s) || s.isBlank() || s.length() > 128)
            throw new IllegalArgumentException("Invalid or missing " + key);
    }
    private static void integer(Map<String, Object> p, String key, int min, int max) {
        number(p.get(key), key, min, max);
    }
    private static int number(Object value, String key, int min, int max) {
        if (!(value instanceof Number n) || !Double.isFinite(n.doubleValue())
            || n.doubleValue() != Math.rint(n.doubleValue()) || n.doubleValue() < min || n.doubleValue() > max)
            throw new IllegalArgumentException(key + " must be an integer in " + min + ".." + max);
        return ((Number) value).intValue();
    }
}
