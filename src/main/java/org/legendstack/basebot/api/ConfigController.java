package org.legendstack.basebot.api;

import org.legendstack.basebot.BotForgeProperties;
import org.legendstack.basebot.PersonaRegistry;
import org.legendstack.basebot.user.BotForgeUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes profile-specific configuration so the React UI
 * can adapt to any persona (Architect, default, etc.).
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final BotForgeProperties properties;
    private final BotForgeUserService userService;
    private final int neo4jHttpPort;
    private final PersonaRegistry personaRegistry;

    public ConfigController(BotForgeProperties properties,
            BotForgeUserService userService,
            @Value("${neo4j.http.port:8892}") int neo4jHttpPort,
            PersonaRegistry personaRegistry) {
        this.properties = properties;
        this.userService = userService;
        this.neo4jHttpPort = neo4jHttpPort;
        this.personaRegistry = personaRegistry;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getConfig() {
        var persona = properties.chat().persona();
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

                // Resolve persona from registry for display name and tagline
                var resolved = personaRegistry.resolve(persona);
                if (resolved.isPresent()) {
                    displayName = resolved.get().displayName();
                    tagline = resolved.get().description();
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
                "availablePersonas", PersonaRegistry.PRESETS));
    }
}
