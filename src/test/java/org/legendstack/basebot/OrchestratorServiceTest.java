package org.legendstack.basebot;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OrchestratorService's structured output records and static
 * behavior.
 * Integration-level tests for execute() require a running AI context and are
 * covered by the Embabel integration test suite.
 */
class OrchestratorServiceTest {

    @Test
    void orchestrationPlanRecordIsWellFormed() {
        var plan = new OrchestratorService.OrchestrationPlan(
                List.of(
                        new OrchestratorService.SubTask("developer", "Write code"),
                        new OrchestratorService.SubTask("assistant", "Summarize")));

        assertEquals(2, plan.steps().size());
        assertEquals("developer", plan.steps().get(0).personaId());
        assertEquals("Write code", plan.steps().get(0).objective());
        assertEquals("assistant", plan.steps().get(1).personaId());
    }

    @Test
    void subTaskResultRecordIsWellFormed() {
        var result = new OrchestratorService.SubTaskResult("The detailed output");
        assertEquals("The detailed output", result.output());
    }

    @Test
    void orchestratorResultRecordIsWellFormed() {
        var result = new OrchestratorService.OrchestratorResult("accumulated context", "developer");
        assertEquals("accumulated context", result.workingMemory());
        assertEquals("developer", result.finalPersonaId());
    }

    @Test
    void emptyPlanStepsAreSafe() {
        var plan = new OrchestratorService.OrchestrationPlan(List.of());
        assertTrue(plan.steps().isEmpty(), "Empty plan should be safe to construct");
    }

    @Test
    void nullSafePlanBehavior() {
        // Null steps list should be allowed by the record
        var plan = new OrchestratorService.OrchestrationPlan(null);
        assertNull(plan.steps(), "Null steps should be allowed (handled at runtime)");
    }
}
