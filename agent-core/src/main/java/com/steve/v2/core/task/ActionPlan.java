package com.steve.v2.core.task;

import java.util.List;

public record ActionPlan(String description, List<Task> tasks) {
    public ActionPlan {
        if (description == null || description.isBlank()) throw new IllegalArgumentException("Missing plan");
        tasks = List.copyOf(tasks);
        if (tasks.isEmpty() || tasks.size() > 32) throw new IllegalArgumentException("Plan requires 1..32 tasks");
        tasks.forEach(TaskSchema::validate);
    }
}
