package com.steve.v2.core;

import com.steve.v2.core.agent.AgentSelection;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AgentSelectionTest {
    @Test void selectsActualSteveInsteadOfOldBobDefault() {
        assertEquals("datmini", AgentSelection.choose("Bob", List.of("datmini")));
    }
    @Test void preservesSelectedSteveWhenMoreThanOneIsVisible() {
        assertEquals("Alex", AgentSelection.choose("alex", List.of("Bob", "Alex")));
    }
    @Test void ambiguousSelectionDoesNotSilentlyPickAnotherPlayerAgent() {
        assertEquals("", AgentSelection.choose("old", List.of("Bob", "Alex")));
    }
    @Test void keepsExplicitNameWhenEntityIsOutsideClientRenderDistance() {
        assertEquals("datmini", AgentSelection.choose("datmini", List.of()));
    }
}
