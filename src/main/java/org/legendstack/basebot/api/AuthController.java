package org.legendstack.basebot.api;

import org.legendstack.basebot.user.DummyBotForgeUserService;
import org.legendstack.basebot.user.BotForgeUser;
import org.legendstack.basebot.user.BotForgeUserEntity;
import org.legendstack.basebot.user.BotForgeUserRepository;
import org.legendstack.basebot.user.BotForgeUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Authentication REST endpoints for the SPA frontend.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final BotForgeUserService userService;
    private final BotForgeUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(BotForgeUserService userService,
            BotForgeUserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public record LoginRequest(String username, String password) {
    }

    public record RegisterRequest(String username, String displayName, String password) {
    }

    public record UserResponse(String id, String displayName, String username, String currentContext) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            var userDetails = userService.loadUserByUsername(request.username());

            // Verify password: BCrypt for JPA users, username==password for dummy users
            boolean passwordValid;
            if (userService instanceof DummyBotForgeUserService) {
                passwordValid = request.password().equals(request.username());
            } else {
                passwordValid = passwordEncoder.matches(request.password(), userDetails.getPassword());
            }

            if (!passwordValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid credentials"));
            }

            var auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            var session = httpRequest.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            var user = userService.getAuthenticatedUser();
            return ResponseEntity.ok(toResponse(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    /**
     * Register a new user account (production mode with JPA persistence).
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username required, password must be at least 8 characters"));
        }

        if (userRepository.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username already taken"));
        }

        String id = UUID.randomUUID().toString();
        String displayName = request.displayName() != null && !request.displayName().isBlank()
                ? request.displayName()
                : request.username();
        String hash = passwordEncoder.encode(request.password());

        var entity = new BotForgeUserEntity(id, request.username(), displayName, hash);
        userRepository.save(entity);

        return ResponseEntity.ok(Map.of(
                "message", "User registered successfully",
                "userId", id,
                "username", request.username()));
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
