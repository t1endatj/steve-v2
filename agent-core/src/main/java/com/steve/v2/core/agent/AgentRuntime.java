package com.steve.v2.core.agent;

import com.steve.v2.core.action.Action;
import com.steve.v2.core.action.ActionResult;
import com.steve.v2.core.perception.WorldSnapshot;
import com.steve.v2.core.planning.Planner;
import com.steve.v2.core.task.ActionPlan;
import com.steve.v2.core.task.Task;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/** Single game-thread owner; asynchronous results are consumed only during tick(). */
public final class AgentRuntime {
    public enum State { IDLE, PLANNING, EXECUTING, PAUSED, FAILED }
    private final Planner planner;
    private final Function<Task, Action> actions;
    private final Consumer<String> output;
    private final Queue<Task> queue = new ArrayDeque<>();
    private CompletableFuture<ActionPlan> pending;
    private Action current;
    private State state = State.IDLE;
    private String goal = "";

    public AgentRuntime(Planner planner, Function<Task, Action> actions, Consumer<String> output) {
        this.planner = planner;
        this.actions = actions;
        this.output = output;
    }
    public void command(WorldSnapshot snapshot, String command) {
        stop();
        try {
            if (command == null || command.isBlank() || command.length() > 2048)
                throw new IllegalArgumentException("Command must contain 1..2048 characters");
            pending = planner.plan(snapshot, command);
            state = State.PLANNING;
            output.accept("Thinking...");
        } catch (Exception e) { fail("Planning failed: " + message(e)); }
    }
    public void submit(ActionPlan plan) {
        stop();
        goal = plan.description();
        queue.addAll(plan.tasks());
        state = State.EXECUTING;
        output.accept(goal);
    }
    public void tick() {
        if (state == State.PAUSED || state == State.FAILED) return;
        try {
            if (state == State.PLANNING) {
                if (pending == null || !pending.isDone()) return;
                ActionPlan plan = pending.join();
                pending = null;
                if (plan == null) throw new IllegalStateException("Planner returned no plan");
                goal = plan.description();
                queue.addAll(plan.tasks());
                state = State.EXECUTING;
                output.accept(goal);
            }
            if (state != State.EXECUTING) return;
            if (current == null && !queue.isEmpty()) {
                current = actions.apply(queue.remove());
                if (current == null) throw new IllegalStateException("No action implementation");
                current.start();
            }
            if (current != null) {
                if (!current.isComplete()) current.tick();
                if (!current.isComplete()) return;
                ActionResult result = current.getResult();
                if (result == null) throw new IllegalStateException("Action completed without result");
                // Always release navigation/flying state after success or failure.
                current.cancel();
                current = null;
                output.accept(result.message());
                if (!result.success()) { fail(result.message()); return; }
            }
            if (queue.isEmpty()) {
                state = State.IDLE;
                goal = "";
                output.accept("Task complete.");
            }
        } catch (Exception e) { fail("Execution failed: " + message(e)); }
    }
    public void stop() {
        if (pending != null) { pending.cancel(true); pending = null; }
        queue.clear();
        if (current != null) {
            Action old = current;
            current = null;
            try { old.cancel(); } catch (Exception e) { output.accept("Cleanup failed: " + message(e)); }
        }
        goal = "";
        state = State.IDLE;
    }
    public void pause() {
        // Planning can be paused too; its result is held until resume.
        if (state == State.EXECUTING || state == State.PLANNING) state = State.PAUSED;
    }
    public void resume() {
        if (state == State.PAUSED) state = pending != null ? State.PLANNING : State.EXECUTING;
    }
    private void fail(String error) {
        stop();
        state = State.FAILED;
        output.accept(error);
    }
    private static String message(Throwable e) {
        while (e.getCause() != null) e = e.getCause();
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
    public State state() { return state; }
    public String goal() { return goal; }
    public int queuedTasks() { return queue.size(); }
    public String currentAction() { return current == null ? "" : current.getDescription(); }
}
