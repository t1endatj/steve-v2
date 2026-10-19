package com.steve.v2.core;

import com.steve.v2.core.action.*;
import com.steve.v2.core.agent.AgentRuntime;
import com.steve.v2.core.perception.WorldSnapshot;
import com.steve.v2.core.task.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class AgentRuntimeTest {
    private static final WorldSnapshot WORLD = new WorldSnapshot("Bob", "overworld", "");
    private static final ActionPlan PLAN = new ActionPlan("Follow", List.of(new Task("follow", Map.of("player", "Alex"))));
    @Test void stopWhilePlanningNeverStartsLateResult() {
        // Simulate a network client that cannot cancel the underlying request.
        CompletableFuture<ActionPlan> future = new CompletableFuture<>() {
            @Override public boolean cancel(boolean interrupt) { return false; }
        };
        AtomicInteger started = new AtomicInteger();
        AgentRuntime runtime = new AgentRuntime((s, c) -> future, t -> { started.incrementAndGet(); return action(true); }, m -> {});
        runtime.command(WORLD, "follow"); runtime.stop(); future.complete(PLAN); runtime.tick();
        assertEquals(0, started.get()); assertEquals(AgentRuntime.State.IDLE, runtime.state());
    }
    @Test void replacementCommandDiscardsOldQueue() {
        AgentRuntime runtime = new AgentRuntime((s, c) -> new CompletableFuture<>(), t -> action(true), m -> {});
        runtime.submit(new ActionPlan("Two tasks", List.of(PLAN.tasks().getFirst(), PLAN.tasks().getFirst())));
        runtime.tick(); assertEquals(1, runtime.queuedTasks());
        runtime.command(WORLD, "new command"); runtime.tick();
        assertEquals(0, runtime.queuedTasks()); assertEquals(AgentRuntime.State.PLANNING, runtime.state());
    }
    @Test void completionClearsGoalAndReturnsIdle() {
        AgentRuntime runtime = new AgentRuntime((s, c) -> CompletableFuture.completedFuture(PLAN), t -> action(true), m -> {});
        runtime.command(WORLD, "follow"); runtime.tick();
        assertEquals("", runtime.goal()); assertEquals(AgentRuntime.State.IDLE, runtime.state());
    }
    @Test void failedActionStopsDependentTasks() {
        AtomicInteger count = new AtomicInteger();
        AgentRuntime runtime = new AgentRuntime((s, c) -> CompletableFuture.completedFuture(PLAN),
            t -> { count.incrementAndGet(); return action(false); }, m -> {});
        runtime.submit(new ActionPlan("Two", List.of(PLAN.tasks().getFirst(), PLAN.tasks().getFirst())));
        runtime.tick(); runtime.tick();
        assertEquals(1, count.get()); assertEquals(AgentRuntime.State.FAILED, runtime.state());
        assertEquals(0, runtime.queuedTasks());
    }
    @Test void pausedPlanningWaitsUntilResume() {
        AgentRuntime runtime = new AgentRuntime((s, c) -> CompletableFuture.completedFuture(PLAN), t -> action(true), m -> {});
        runtime.command(WORLD, "follow"); runtime.pause(); runtime.tick();
        assertEquals(AgentRuntime.State.PAUSED, runtime.state());
        runtime.resume(); runtime.tick(); assertEquals(AgentRuntime.State.IDLE, runtime.state());
    }
    private static Action action(boolean success) {
        return new Action() {
            boolean done;
            public void start() {}
            public void tick() { done = true; }
            public void cancel() {}
            public boolean isComplete() { return done; }
            public ActionResult getResult() { return success ? ActionResult.success("OK") : ActionResult.failure("Blocked"); }
            public String getDescription() { return "Test action"; }
        };
    }
}
