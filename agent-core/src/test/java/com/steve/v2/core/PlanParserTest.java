package com.steve.v2.core;

import com.steve.v2.core.planning.PlanParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlanParserTest {
    @Test void validMinePreservesQuantity() {
        var plan = PlanParser.parse("""
            {"plan":"Mine iron","tasks":[{"action":"mine","parameters":{"block":"iron","quantity":20}}]}
            """);
        assertEquals(20, plan.tasks().getFirst().getIntParameter("quantity", 8));
    }
    @Test void rejectsOldInconsistentParameterNames() {
        assertThrows(IllegalArgumentException.class, () -> PlanParser.parse("""
            {"plan":"Mine","tasks":[{"action":"mine","parameters":{"blockType":"iron","count":20}}]}
            """));
    }
    @Test void rejectsPartialPlanWithUnknownAction() {
        assertThrows(IllegalArgumentException.class, () -> PlanParser.parse("""
            {"plan":"Build","tasks":[{"action":"follow","parameters":{"player":"Alex"}},{"action":"craft","parameters":{"item":"pickaxe","quantity":1}}]}
            """));
    }
    @Test void rejectsFractionalOrNegativeQuantity() {
        for (String qty : new String[]{"-1", "0", "1.5", "999999999999999999999"}) {
            assertThrows(IllegalArgumentException.class, () -> PlanParser.parse(
                "{\"plan\":\"Mine\",\"tasks\":[{\"action\":\"mine\",\"parameters\":{\"block\":\"iron\",\"quantity\":" + qty + "}}]}"));
        }
    }
    @Test void rejectsMissingCoordinatesRatherThanDefaultingToOrigin() {
        assertThrows(IllegalArgumentException.class, () -> PlanParser.parse("""
            {"plan":"Move","tasks":[{"action":"pathfind","parameters":{"x":2,"z":3}}]}
            """));
    }
    @Test void acceptsMarkdownWrappedJson() {
        assertEquals("Follow", PlanParser.parse("```json\n{\"plan\":\"Follow\",\"tasks\":[{\"action\":\"follow\",\"parameters\":{\"player\":\"Alex\"}}]}\n```").description());
    }
}
