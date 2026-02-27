package org.legendstack.basebot.user;

import com.embabel.agent.api.identity.UserService;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Service for managing BotForge users.
 * Extends UserDetailsService so a single bean drives both
 * application users and Spring Security authentication.
 */
public interface BotForgeUserService extends UserService<BotForgeUser>, UserDetailsService {

    /**
     * Get the currently authenticated user.
     */
    BotForgeUser getAuthenticatedUser();

}
