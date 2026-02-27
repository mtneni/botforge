package org.legendstack.basebot.api;

import org.legendstack.basebot.rag.DocumentService;
import org.legendstack.basebot.user.BotForgeUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Map;

/**
 * REST endpoints for user context management.
 */
@RestController
@RequestMapping("/api/contexts")
public class ContextController {

    private final DocumentService documentService;
    private final BotForgeUserService userService;

    public ContextController(DocumentService documentService, BotForgeUserService userService) {
        this.documentService = documentService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> listContexts() {
        var user = userService.getAuthenticatedUser();
        var prefix = user.getId() + "_";
        var contextNames = new ArrayList<>(
                documentService.contexts().stream()
                        .filter(ctx -> ctx.startsWith(prefix))
                        .map(ctx -> ctx.substring(prefix.length()))
                        .distinct()
                        .toList());
        if (!contextNames.contains(user.getCurrentContextName())) {
            contextNames.add(0, user.getCurrentContextName());
        }
        return ResponseEntity.ok(Map.of(
                "contexts", contextNames,
                "current", user.getCurrentContextName()));
    }

    public record ContextRequest(String context) {
    }

    @PutMapping("/current")
    public ResponseEntity<?> switchContext(@RequestBody ContextRequest request) {
        var user = userService.getAuthenticatedUser();
        user.setCurrentContextName(request.context());
        return ResponseEntity.ok(Map.of(
                "current", user.getCurrentContextName(),
                "effectiveContext", user.effectiveContext()));
    }
}
