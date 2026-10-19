package com.steve.v2.core.planning;

import com.steve.v2.core.perception.WorldSnapshot;
import com.steve.v2.core.task.ActionPlan;
import java.util.concurrent.CompletableFuture;

public interface Planner {
    CompletableFuture<ActionPlan> plan(WorldSnapshot snapshot, String command);
}
