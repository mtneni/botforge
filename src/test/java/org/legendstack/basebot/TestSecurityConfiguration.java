package org.legendstack.basebot;

import com.embabel.agent.rag.service.NamedEntityDataRepository;
import org.legendstack.basebot.user.DummyBotForgeUserService;
import org.legendstack.basebot.user.BotForgeUser;
import org.legendstack.basebot.user.BotForgeUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the {@link BotForgeUserService} bean that the excluded
 * {@code SecurityConfiguration} normally supplies.
 * No web security is needed since the test runs with {@code web-application-type=none}.
 */
@Configuration
class TestSecurityConfiguration {

    @Bean
    BotForgeUserService userService(NamedEntityDataRepository entityRepository) {
        return new DummyBotForgeUserService(entityRepository,
                new BotForgeUser("1", "Alice Agu", "alice"),
                new BotForgeUser("2", "Ben Blossom", "ben")
        );
    }
}
