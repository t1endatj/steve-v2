package com.steve.v2.core;

import com.steve.v2.core.agent.WorkQueue;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WorkQueueTest {
    @Test void singleWorkerCanFinishEntireBuild() {
        var work = new WorkQueue<>(List.of(1, 2, 3, 4, 5));
        for (int i = 1; i <= 5; i++) { assertEquals(i, work.claim("Steve")); work.commit("Steve"); }
        assertTrue(work.isComplete());
    }
    @Test void claimedBlockDoesNotCountAsPlaced() {
        var work = new WorkQueue<>(List.of(1));
        work.claim("Steve"); assertFalse(work.isComplete()); assertEquals(0, work.completed());
        work.commit("Steve"); assertTrue(work.isComplete());
    }
    @Test void abandonedWorkCanBePickedUpByAnotherAgent() {
        var work = new WorkQueue<>(List.of(1, 2));
        assertEquals(1, work.claim("Steve")); assertEquals(2, work.claim("Bob"));
        work.release("Steve"); work.commit("Bob");
        assertEquals(1, work.claim("Bob")); work.commit("Bob"); assertTrue(work.isComplete());
    }
}
