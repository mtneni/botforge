package org.legendstack.basebot.api;

import org.legendstack.basebot.BotForgeProperties;
import org.legendstack.basebot.user.BotForgeUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes profile-specific configuration so the React UI
 * can adapt to any persona (Astrid, default, etc.).
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final BotForgeProperties properties;
    private final BotForgeUserService userService;
    private final int neo4jHttpPort;
    private final CustomPersonaRepository customPersonaRepository;

    public ConfigController(BotForgeProperties properties,
            BotForgeUserService userService,
            @Value("${neo4j.http.port:8892}") int neo4jHttpPort,
            CustomPersonaRepository customPersonaRepository) {
        this.properties = properties;
        this.userService = userService;
        this.neo4jHttpPort = neo4jHttpPort;
        this.customPersonaRepository = customPersonaRepository;
    }

    @GetMapping
    public ResponseEntity<?> getConfig() {
        var user = properties.chat().persona();
        var persona = user;
        var displayName = persona.substring(0, 1).toUpperCase() + persona.substring(1);
        var tagline = properties.chat().tagline();

        // Check for authenticated user overrides
        try {
            var authUser = userService.getAuthenticatedUser();
            if (authUser != null) {
                if (authUser.getPersonaOverride() != null) {
                    persona = authUser.getPersonaOverride();
                    displayName = persona.substring(0, 1).toUpperCase() + persona.substring(1);
                }

                // Try to find matching preset for tagline
                boolean found = false;
                for (PersonaController.PersonaPreset preset : PersonaController.PRESETS) {
                    if (preset.id().equals(persona)) {
                        displayName = preset.displayName();
                        tagline = preset.description();
                        found = true;
                        break;
                    }
                }

                if (!found && persona.startsWith("custom_")) {
                    var customOpt = customPersonaRepository.findById(persona);
                    if (customOpt.isPresent()) {
                        displayName = customOpt.get().getDisplayName();
                        tagline = customOpt.get().getDescription();
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to defaults if no user service access (e.g. during startup)
        }

        var stylesheet = properties.stylesheet();
        var logoUrl = (stylesheet != null && !stylesheet.isBlank())
                ? "/images/" + stylesheet + ".jpg"
                : "/images/weltenchronik.jpg";

        return ResponseEntity.ok(Map.of(
                "persona", persona,
                "displayName", displayName,
                "tagline", tagline,
                "stylesheet", stylesheet != null ? stylesheet : "",
                "logoUrl", logoUrl,
                "neo4jHttpPort", neo4jHttpPort,
                "memoryEnabled", properties.memory().enabled(),
                "availablePersonas", PersonaController.PRESETS));
    }
}
