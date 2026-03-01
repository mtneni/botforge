package org.legendstack.basebot;

import org.legendstack.basebot.api.CustomPersona;
import org.legendstack.basebot.api.CustomPersonaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonaRegistryTest {

    @Mock
    private CustomPersonaRepository customPersonaRepository;

    @InjectMocks
    private PersonaRegistry registry;

    @Test
    void resolveBuiltInPreset() {
        var result = registry.resolve("assistant");
        assertTrue(result.isPresent(), "Should resolve built-in 'assistant' preset");
        assertEquals("Assistant", result.get().displayName());
        assertEquals("qa", result.get().objective());
    }

    @Test
    void resolveOrchestratorPreset() {
        var result = registry.resolve("orchestrator");
        assertTrue(result.isPresent());
        assertEquals("Orchestrator", result.get().displayName());
    }

    @Test
    void resolveCustomPersonaFallback() {
        var custom = new CustomPersona("custom_123", "user1", "My Bot",
                "custom_obj", "default", "A custom bot", "terminal");
        when(customPersonaRepository.findById("custom_123")).thenReturn(Optional.of(custom));

        var result = registry.resolve("custom_123");
        assertTrue(result.isPresent(), "Should fall back to custom persona repo");
        assertEquals("My Bot", result.get().displayName());
        assertEquals("custom_obj", result.get().objective());
    }

    @Test
    void resolveUnknownReturnsEmpty() {
        when(customPersonaRepository.findById("nonexistent")).thenReturn(Optional.empty());
        var result = registry.resolve("nonexistent");
        assertTrue(result.isEmpty(), "Unknown persona should return empty");
    }

    @Test
    void allForUserIncludesPresetsAndCustom() {
        var custom = new CustomPersona("custom_1", "user1", "Custom One",
                "obj1", "default", "Custom persona", "terminal");
        when(customPersonaRepository.findByUserId("user1")).thenReturn(List.of(custom));

        var all = registry.allForUser("user1");

        // Should include all presets + 1 custom
        assertTrue(all.size() > PersonaRegistry.PRESETS.size(),
                "Should include presets plus custom personas");
        assertTrue(all.stream().anyMatch(p -> p.id().equals("custom_1")),
                "Custom persona should be in the list");
        assertTrue(all.stream().anyMatch(p -> p.id().equals("assistant")),
                "Built-in assistant should be in the list");
    }

    @Test
    void presetsAreImmutable() {
        int originalSize = PersonaRegistry.PRESETS.size();
        assertThrows(UnsupportedOperationException.class,
                () -> PersonaRegistry.PRESETS.add(new PersonaRegistry.PersonaPreset(
                        "test", "Test", "obj", "beh", "desc", "icon")),
                "PRESETS list should be immutable");
        assertEquals(originalSize, PersonaRegistry.PRESETS.size());
    }

    @Test
    void saveCustomDelegatesToRepository() {
        var custom = new CustomPersona("custom_x", "user1", "Bot X",
                "obj", "default", "desc", "icon");
        when(customPersonaRepository.save(custom)).thenReturn(custom);

        var result = registry.saveCustom(custom);
        assertEquals(custom, result);
        verify(customPersonaRepository).save(custom);
    }

    @Test
    void deleteCustomDelegatesToRepository() {
        registry.deleteCustom("custom_x");
        verify(customPersonaRepository).deleteById("custom_x");
    }
}
