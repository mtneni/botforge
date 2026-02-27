package org.legendstack.basebot.user;

import com.embabel.agent.rag.model.SimpleNamedEntityData;
import com.embabel.agent.rag.service.NamedEntityDataRepository;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hardcoded user service implementation for development/demo purposes.
 * DO NOT USE IN PRODUCTION - implement BotForgeUserService with proper user and
 * password management.
 */
public class DummyBotForgeUserService implements BotForgeUserService {

    private static final Logger logger = LoggerFactory.getLogger(DummyBotForgeUserService.class);

    private final List<BotForgeUser> users;
    private final NamedEntityDataRepository entityRepository;
    private final Set<String> persistedUserIds = ConcurrentHashMap.newKeySet();

    public DummyBotForgeUserService(NamedEntityDataRepository entityRepository,
            BotForgeUser... users) {
        this.users = Arrays.stream(users).toList();
        this.entityRepository = entityRepository;
    }

    @Override
    public BotForgeUser getAuthenticatedUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails userDetails) {
            var user = users.stream().filter(u -> Objects.equals(u.getUsername(), userDetails.getUsername()))
                    .findFirst();
            if (user.isPresent()) {
                ensureEntityExists(user.get());
                return user.get();
            }
        }
        // Return anonymous user if not authenticated
        return new BotForgeUser(UUID.randomUUID().toString(), "Anonymous", "anonymous");
    }

    private void ensureEntityExists(BotForgeUser user) {
        if (persistedUserIds.add(user.getId())) {
            entityRepository.save(new SimpleNamedEntityData(
                    user.getId(), null, user.getName(), user.getDescription(),
                    user.labels(), Map.of(), Map.of(), null));
            logger.info("Persisted user entity to graph: {} ({})", user.getName(), user.getId());
        }
    }

    public List<BotForgeUser> getUsers() {
        return users;
    }

    @Override
    @Nullable
    public BotForgeUser findById(@NonNull String id) {
        return users.stream().filter(u -> Objects.equals(u.getId(), id)).findFirst().orElse(null);
    }

    @Override
    @Nullable
    public BotForgeUser findByUsername(@NonNull String username) {
        return users.stream().filter(u -> Objects.equals(u.getUsername(), username)).findFirst().orElse(null);
    }

    @Override
    @Nullable
    public BotForgeUser findByEmail(@NonNull String email) {
        return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        return User.withDefaultPasswordEncoder()
                .username(user.getUsername())
                .password(user.getUsername())
                .roles("USER")
                .build();
    }
}
