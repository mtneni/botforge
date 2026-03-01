package org.legendstack.basebot;

import org.legendstack.basebot.api.CustomPersona;
import org.legendstack.basebot.api.CustomPersonaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Single source of truth for persona lookup — presets and user-created custom
 * personas.
 * Replaces the duplicated lookup logic that was scattered across ChatActions
 * and PersonaController.
 */
@Service
public class PersonaRegistry {

        private final CustomPersonaRepository customPersonaRepository;

        /**
         * Built-in persona presets. Each preset maps to a corresponding .jinja template
         * file.
         */
        public static final List<PersonaPreset> PRESETS = List.of(
                        new PersonaPreset("assistant", "Assistant", "qa", "default",
                                        "Thoughtful, precise knowledge assistant", "sparkles"),
                        new PersonaPreset("security", "Security", "security", "default",
                                        "Deep code audits and compliance checks", "shield"),
                        new PersonaPreset("developer", "Developer", "developer", "default",
                                        "Fast reasoning profile optimized for coding tasks", "zap"),
                        new PersonaPreset("orchestrator", "Orchestrator", "orchestrator", "default",
                                        "Intelligent router that delegates to specialized identities", "network"));

        public record PersonaPreset(
                        String id,
                        String displayName,
                        String objective,
                        String behaviour,
                        String description,
                        String icon) {
        }

        public PersonaRegistry(CustomPersonaRepository customPersonaRepository) {
                this.customPersonaRepository = customPersonaRepository;
        }

        /**
         * Resolve a persona by ID, checking built-in presets first, then user-created
         * custom personas.
         */
        public Optional<PersonaPreset> resolve(String personaId) {
                var preset = PRESETS.stream()
                                .filter(p -> p.id().equals(personaId))
                                .findFirst();
                if (preset.isPresent()) {
                        return preset;
                }
                return customPersonaRepository.findById(personaId)
                                .map(c -> new PersonaPreset(c.getId(), c.getDisplayName(),
                                                c.getObjective(), c.getBehaviour(), c.getDescription(), c.getIcon()));
        }

        /**
         * List all available personas for a user (presets + their custom ones).
         */
        public List<PersonaPreset> allForUser(String userId) {
                var custom = customPersonaRepository.findByUserId(userId).stream()
                                .map(c -> new PersonaPreset(c.getId(), c.getDisplayName(),
                                                c.getObjective(), c.getBehaviour(), c.getDescription(), c.getIcon()))
                                .toList();
                return Stream.concat(PRESETS.stream(), custom.stream()).toList();
        }

        /**
         * Save a custom persona.
         */
        public CustomPersona saveCustom(CustomPersona persona) {
                return customPersonaRepository.save(persona);
        }

        /**
         * Delete a custom persona by ID.
         */
        public void deleteCustom(String id) {
                customPersonaRepository.deleteById(id);
        }

        /**
         * Find a custom persona by ID (returns raw entity for mutation).
         */
        public Optional<CustomPersona> findCustomById(String id) {
                return customPersonaRepository.findById(id);
        }
}
