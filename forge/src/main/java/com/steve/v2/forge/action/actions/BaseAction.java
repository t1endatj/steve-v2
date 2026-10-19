package com.steve.v2.forge.action.actions;

import com.steve.v2.core.action.ActionResult;
import com.steve.v2.core.task.Task;
import com.steve.v2.forge.entity.SteveEntity;

public abstract class BaseAction implements com.steve.v2.core.action.Action {
    protected final SteveEntity steve;
    protected final Task task;
    protected ActionResult result;
    protected boolean started = false;
    protected boolean cancelled = false;

    public BaseAction(SteveEntity steve, Task task) {
        this.steve = steve;
        this.task = task;
    }

    public void start() {
        if (started) return;
        started = true;
        onStart();
    }

    public void tick() {
        if (!started || isComplete()) return;
        onTick();
    }

    public void cancel() {
        cancelled = true;
        if (result == null) result = ActionResult.failure("Action cancelled");
        onCancel();
    }

    public boolean isComplete() {
        return result != null || cancelled;
    }

    public ActionResult getResult() {
        return result;
    }

    protected abstract void onStart();
    protected abstract void onTick();
    protected abstract void onCancel();
    
    public abstract String getDescription();
}


