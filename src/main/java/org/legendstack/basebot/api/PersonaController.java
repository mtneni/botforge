package org.legendstack.basebot.api;

import org.legendstack.basebot.BotForgeProperties;
import org.legendstack.basebot.PersonaRegistry;
import org.legendstack.basebot.PersonaRegistry.PersonaPreset;
import org.legendstack.basebot.user.BotForgeUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for the Persona Studio — listing, switching, and querying
 * active persona configurations per session.
 */
@RestController
@RequestMapping("/api/personas")
public class PersonaController {

        private final BotForgeUserService userService;
        private final BotForgeProperties properties;
        private final PersonaRegistry personaRegistry;

        public PersonaController(BotForgeUserService userService, BotForgeProperties properties,
                        PersonaRegistry personaRegistry) {
                this.userService = userService;
                this.properties = properties;
                this.personaRegistry = personaRegistry;
        }

        public record PersonaCreateRequest(
                        String displayName,
                        String objective,
                        String description) {
        }

        public record SwitchRequest(String personaId) {
        }

        @GetMapping
        public ResponseEntity<List<Map<String, Object>>> listPersonas() {
                var user = userService.getAuthenticatedUser();
                var activeId = user.getPersonaOverride() != null
                                ? user.getPersonaOverride()
                                : properties.chat().persona();

                var allPersonas = personaRegistry.allForUser(user.getId());

                var result = allPersonas.stream()
                                .map(p -> Map.<String, Object>of(
                                                "id", p.id(),
                                                "displayName", p.displayName(),
                                                "objective", p.objective(),
                                                "behaviour", p.behaviour(),
                                                "description", p.description(),
                                                "icon", p.icon(),
                                                "active", p.id().equals(activeId)))
                                .toList();
                return ResponseEntity.ok(result);
        }

        @PostMapping
        public ResponseEntity<Map<String, Object>> createPersona(@RequestBody PersonaCreateRequest request) {
                var user = userService.getAuthenticatedUser();
                var id = "custom_" + System.currentTimeMillis();

                personaRegistry.saveCustom(new CustomPersona(id, user.getId(), request.displayName(),
                                request.objective(), "default", request.description(), "terminal"));

                var preset = new PersonaPreset(id, request.displayName(), request.objective(),
                                "default", request.description(), "terminal");

                return ResponseEntity.ok(Map.of(
                                "message", "Persona forged: " + preset.displayName(),
                                "persona", preset));
        }

        @PutMapping("/{id}")
        public ResponseEntity<Map<String, Object>> updatePersona(@PathVariable String id,
                        @RequestBody PersonaCreateRequest request) {
                var user = userService.getAuthenticatedUser();
                var customPersonaOpt = personaRegistry.findCustomById(id);

                if (customPersonaOpt.isEmpty() || !customPersonaOpt.get().getUserId().equals(user.getId())) {
                        return ResponseEntity.notFound().build();
                }

                CustomPersona customPersona = customPersonaOpt.get();
                customPersona.setDisplayName(request.displayName());
                customPersona.setObjective(request.objective());
                customPersona.setDescription(request.description());
                personaRegistry.saveCustom(customPersona);

                var updated = new PersonaPreset(id, request.displayName(), request.objective(),
                                customPersona.getBehaviour(), request.description(), customPersona.getIcon());

                // If this was the active persona, update overrides too
                if (id.equals(user.getPersonaOverride())) {
                        user.setPersonaOverride(updated.id());
                        user.setObjectiveOverride(updated.objective());
                }

                return ResponseEntity.ok(Map.of(
                                "message", "Persona recalibrated: " + updated.displayName(),
                                "persona", updated));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Map<String, String>> deletePersona(@PathVariable String id) {
                var user = userService.getAuthenticatedUser();
                var customPersonaOpt = personaRegistry.findCustomById(id);

                if (customPersonaOpt.isPresent() && customPersonaOpt.get().getUserId().equals(user.getId())) {
                        personaRegistry.deleteCustom(id);
                        if (id.equals(user.getPersonaOverride())) {
                                user.setPersonaOverride(null);
                                user.setObjectiveOverride(null);
                        }
                }

                return ResponseEntity.ok(Map.of("message", "Persona archived."));
        }

        @PostMapping("/active")
        public ResponseEntity<Map<String, String>> switchPersona(@RequestBody SwitchRequest request) {
                var user = userService.getAuthenticatedUser();
                var preset = personaRegistry.resolve(request.personaId()).orElse(null);

                if (preset == null) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("error", "Unknown persona: " + request.personaId()));
                }

                user.setPersonaOverride(preset.id());
                user.setObjectiveOverride(preset.objective());
                user.setBehaviourOverride(preset.behaviour());

                return ResponseEntity.ok(Map.of(
                                "persona", preset.id(),
                                "displayName", preset.displayName(),
                                "tagline", preset.description(),
                                "message", "Switched to " + preset.displayName()));
        }

        @GetMapping("/active")
        public ResponseEntity<Map<String, String>> getActivePersona() {
                var user = userService.getAuthenticatedUser();
                var activeId = user.getPersonaOverride() != null
                                ? user.getPersonaOverride()
                                : properties.chat().persona();

                var preset = personaRegistry.resolve(activeId)
                                .orElse(PersonaRegistry.PRESETS.get(0));

                return ResponseEntity.ok(Map.of(
                                "id", preset.id(),
                                "displayName", preset.displayName(),
                                "description", preset.description()));
        }
}
