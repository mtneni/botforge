package org.legendstack.basebot.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link BotForgeUserEntity}.
 */
public interface BotForgeUserRepository extends JpaRepository<BotForgeUserEntity, String> {

    Optional<BotForgeUserEntity> findByUsername(String username);

    Optional<BotForgeUserEntity> findByEmail(String email);
}
