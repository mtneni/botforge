package org.legendstack.basebot.api;

import org.legendstack.basebot.user.DummyBotForgeUserService;
import org.legendstack.basebot.user.BotForgeUser;
import org.legendstack.basebot.user.BotForgeUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Authentication REST endpoints for the SPA frontend.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final BotForgeUserService userService;

    public AuthController(BotForgeUserService userService) {
        this.userService = userService;
    }

    public record LoginRequest(String username, String password) {
    }

    public record UserResponse(String id, String displayName, String username, String currentContext) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            var userDetails = userService.loadUserByUsername(request.username());
            // For DummyBotForgeUserService, password == username
            if (!request.password().equals(request.username())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid credentials"));
            }

            var auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Create session
            var session = httpRequest.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            var user = userService.getAuthenticatedUser();
            return ResponseEntity.ok(toResponse(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        try {
            var user = userService.getAuthenticatedUser();
            if ("anonymous".equals(user.getUsername())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Not authenticated"));
            }
            return ResponseEntity.ok(toResponse(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers() {
        if (userService instanceof DummyBotForgeUserService dummy) {
            var users = dummy.getUsers().stream()
                    .map(u -> Map.of("username", u.getUsername(), "displayName", u.getDisplayName()))
                    .toList();
            return ResponseEntity.ok(users);
        }
        return ResponseEntity.ok(List.of());
    }

    private UserResponse toResponse(BotForgeUser user) {
        return new UserResponse(user.getId(), user.getDisplayName(), user.getUsername(), user.getCurrentContextName());
    }
}
