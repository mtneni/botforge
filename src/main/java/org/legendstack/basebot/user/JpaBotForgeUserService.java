package org.legendstack.basebot.user;

import com.embabel.agent.rag.model.SimpleNamedEntityData;
import com.embabel.agent.rag.service.NamedEntityDataRepository;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

/**
 * Production-ready user service backed by PostgreSQL via JPA.
 * Active when NOT in the "dev" profile.
 * <p>
 * Passwords are stored as BCrypt hashes in the {@code botforge_users} table.
 */
@Service
@Profile("!dev")
public class JpaBotForgeUserService implements BotForgeUserService {

    private static final Logger logger = LoggerFactory.getLogger(JpaBotForgeUserService.class);

    private final BotForgeUserRepository userRepository;
    private final NamedEntityDataRepository entityRepository;
    private final Set<String> persistedUserIds = ConcurrentHashMap.newKeySet();

    public JpaBotForgeUserService(BotForgeUserRepository userRepository,
            NamedEntityDataRepository entityRepository) {
        this.userRepository = userRepository;
        this.entityRepository = entityRepository;
    }

    @Override
    public BotForgeUser getAuthenticatedUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails userDetails) {
            var user = findByUsername(userDetails.getUsername());
            if (user != null) {
                ensureEntityExists(user);
                return user;
            }
        }
        return new BotForgeUser(UUID.randomUUID().toString(), "Anonymous", "anonymous");
    }

    @Override
    @Nullable
    public BotForgeUser findById(@NonNull String id) {
        return userRepository.findById(id)
                .map(BotForgeUserEntity::toDomainUser)
                .orElse(null);
    }

    @Override
    @Nullable
    public BotForgeUser findByUsername(@NonNull String username) {
        return userRepository.findByUsername(username)
                .map(BotForgeUserEntity::toDomainUser)
                .orElse(null);
    }

    @Override
    @Nullable
    public BotForgeUser findByEmail(@NonNull String email) {
        return userRepository.findByEmail(email)
                .map(BotForgeUserEntity::toDomainUser)
                .orElse(null);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var entity = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return User.builder()
                .username(entity.getUsername())
                .password(entity.getPasswordHash()) // BCrypt encoded
                .roles(entity.getRole())
                .build();
    }

    private void ensureEntityExists(BotForgeUser user) {
        if (persistedUserIds.add(user.getId())) {
            entityRepository.save(new SimpleNamedEntityData(
                    user.getId(), null, user.getName(), user.getDescription(),
                    user.labels(), Map.of(), Map.of(), null));
            logger.info("Persisted user entity to graph: {} ({})", user.getName(), user.getId());
        }
    }
}
