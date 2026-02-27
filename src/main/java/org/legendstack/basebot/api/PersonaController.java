package org.legendstack.basebot.api;

import org.legendstack.basebot.BotForgeProperties;
import org.legendstack.basebot.user.BotForgeUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * REST endpoints for the Persona Studio — listing, switching, and querying
 * active persona configurations per session.
 */
@RestController
@RequestMapping("/api/personas")
public class PersonaController {

        private final BotForgeUserService userService;
        private final BotForgeProperties properties;
        private final CustomPersonaRepository customPersonaRepository;

        public PersonaController(BotForgeUserService userService, BotForgeProperties properties,
                        CustomPersonaRepository customPersonaRepository) {
                this.userService = userService;
                this.properties = properties;
                this.customPersonaRepository = customPersonaRepository;
        }

        /**
         * Registry of available persona presets.
         * New personas are added here + a matching .jinja template file.
         */
        public static final List<PersonaPreset> PRESETS = List.of(
                        new PersonaPreset("assistant", "Assistant", "qa", "default",
                                        "Thoughtful, precise knowledge assistant",
                                        "sparkles"),
                        new PersonaPreset("astrid", "Astrid", "astrid", "default",
                                        "Warm, mystical Australian astrologer with horoscope tools",
                                        "moon"),
                        new PersonaPreset("security", "Security", "security", "default",
                                        "Deep code audits and compliance checks",
                                        "shield"),
                        new PersonaPreset("developer", "Developer", "developer", "default",
                                        "Fast reasoning profile optimized for coding tasks",
                                        "zap"),
                        new PersonaPreset("orchestrator", "Orchestrator", "orchestrator", "default",
                                        "Intelligent router that delegates to specialized identities",
                                        "network"));

        public record PersonaPreset(
                        String id,
                        String displayName,
                        String objective,
                        String behaviour,
                        String description,
                        String icon) {
        }

        public record PersonaCreateRequest(
                        String displayName,
                        String objective,
                        String description) {
        }

        public record DryRunRequest(
                        String objective,
                        String message,
                        List<Map<String, String>> history) {
        }

        public record SwitchRequest(String personaId) {
        }

        private List<PersonaPreset> getCustomPersonas(String userId) {
                return customPersonaRepository.findByUserId(userId).stream()
                                .map(c -> new PersonaPreset(c.getId(), c.getDisplayName(), c.getObjective(),
                                                c.getBehaviour(), c.getDescription(), c.getIcon()))
                                .toList();
        }

        @GetMapping
        public ResponseEntity<?> listPersonas() {
                var user = userService.getAuthenticatedUser();
                var activeId = user.getPersonaOverride() != null
                                ? user.getPersonaOverride()
                                : properties.chat().persona();

                var customPersonas = getCustomPersonas(user.getId());

                var result = Stream.concat(PRESETS.stream(), customPersonas.stream())
                                .map(p -> Map.of(
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
        public ResponseEntity<?> createPersona(@RequestBody PersonaCreateRequest request) {
                var user = userService.getAuthenticatedUser();
                var id = "custom_" + System.currentTimeMillis();
                var preset = new PersonaPreset(
                                id,
                                request.displayName(),
                                request.objective(),
                                "default",
                                request.description(),
                                "terminal");

                customPersonaRepository.save(new CustomPersona(id, user.getId(), request.displayName(),
                                request.objective(), "default", request.description(), "terminal"));

                return ResponseEntity.ok(Map.of(
                                "message", "Persona forged: " + preset.displayName(),
                                "persona", preset));
        }

        @PutMapping("/{id}")
        public ResponseEntity<?> updatePersona(@PathVariable String id, @RequestBody PersonaCreateRequest request) {
                var user = userService.getAuthenticatedUser();
                var customPersonaOpt = customPersonaRepository.findById(id);

                if (customPersonaOpt.isEmpty() || !customPersonaOpt.get().getUserId().equals(user.getId())) {
                        return ResponseEntity.notFound().build();
                }

                CustomPersona customPersona = customPersonaOpt.get();
                customPersona.setDisplayName(request.displayName());
                customPersona.setObjective(request.objective());
                customPersona.setDescription(request.description());
                customPersonaRepository.save(customPersona);

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
        public ResponseEntity<?> deletePersona(@PathVariable String id) {
                var user = userService.getAuthenticatedUser();
                var customPersonaOpt = customPersonaRepository.findById(id);

                if (customPersonaOpt.isPresent() && customPersonaOpt.get().getUserId().equals(user.getId())) {
                        customPersonaRepository.deleteById(id);
                        if (id.equals(user.getPersonaOverride())) {
                                user.setPersonaOverride(null);
                                user.setObjectiveOverride(null);
                        }
                }

                return ResponseEntity.ok(Map.of("message", "Persona archived."));
        }

        @PostMapping("/active")
        public ResponseEntity<?> switchPersona(@RequestBody SwitchRequest request) {
                var user = userService.getAuthenticatedUser();
                var customPersonas = getCustomPersonas(user.getId());

                var preset = Stream.concat(PRESETS.stream(), customPersonas.stream())
                                .filter(p -> p.id().equals(request.personaId()))
                                .findFirst()
                                .orElse(null);

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
        public ResponseEntity<?> getActivePersona() {
                var user = userService.getAuthenticatedUser();
                var activeId = user.getPersonaOverride() != null
                                ? user.getPersonaOverride()
                                : properties.chat().persona();

                var customPersonas = getCustomPersonas(user.getId());

                var preset = Stream.concat(PRESETS.stream(), customPersonas.stream())
                                .filter(p -> p.id().equals(activeId))
                                .findFirst()
                                .orElse(PRESETS.get(0));

                return ResponseEntity.ok(Map.of(
                                "id", preset.id(),
                                "displayName", preset.displayName(),
                                "description", preset.description()));
        }

        @PostMapping("/dry-run")
        public ResponseEntity<?> dryRun(@RequestBody DryRunRequest request) {
                // This is a minimal implementation that doesn't save to DB
                // In a real scenario, this would use a temporary Embabel session
                // For now, we simulate the personality by echoing or using a simple completion

                String reply = "Simulation Response: " +
                                (request.objective().toLowerCase().contains("creative")
                                                ? "I'm feeling very creative today! "
                                                : "Awaiting directives. ")
                                +
                                "I received your message: '" + request.message() + "'";

                // Note: In the final version, this should actually call the RAGEngine
                // with a custom Persona.

                return ResponseEntity.ok(Map.of("reply", reply));
        }
}
