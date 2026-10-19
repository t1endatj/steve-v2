package com.steve.v2.core.agent;

import java.util.*;

/** Game-thread work leasing: claim is separate from commit; failed/unloaded workers release work. */
public final class WorkQueue<T> {
    private final ArrayDeque<T> ready;
    private final Map<String, T> leases = new HashMap<>();
    private final int total;
    private int completed;
    public WorkQueue(List<T> work) {
        ready = new ArrayDeque<>(work); total = work.size();
        if (total == 0) throw new IllegalArgumentException("Empty work queue");
    }
    public T claim(String worker) {
        if (leases.containsKey(worker)) return leases.get(worker);
        T next = ready.pollFirst();
        if (next != null) leases.put(worker, next);
        return next;
    }
    public void commit(String worker) {
        if (leases.remove(worker) == null) throw new IllegalStateException("No lease for worker");
        completed++;
    }
    public void release(String worker) {
        T work = leases.remove(worker);
        if (work != null) ready.addFirst(work);
    }
    public int completed() { return completed; }
    public int total() { return total; }
    public boolean isComplete() { return completed == total; }
}
