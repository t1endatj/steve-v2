package com.steve.v2.core.action;

/** All methods are called on the owning game thread. */
public interface Action {
    void start();
    void tick();
    void cancel();
    boolean isComplete();
    ActionResult getResult();
    String getDescription();
}
