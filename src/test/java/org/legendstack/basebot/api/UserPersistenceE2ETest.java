package org.legendstack.basebot.api;

import org.legendstack.basebot.user.BotForgeUserEntity;
import org.legendstack.basebot.user.BotForgeUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E test for user persistence with a real PostgreSQL container.
 * Verifies the JPA entity, repository, and password hashing work
 * correctly against a real database — not mocks.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = UserPersistenceE2ETest.TestConfig.class)
class UserPersistenceE2ETest {

    /**
     * Minimal Spring context that only scans the user entity and repository,
     * avoiding the full BotForge/Embabel application boot.
     */
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = BotForgeUserEntity.class)
    @EnableJpaRepositories(basePackageClasses = BotForgeUserRepository.class)
    static class TestConfig {
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("botforge_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private BotForgeUserRepository userRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void entityIsPersisted() {
        var entity = new BotForgeUserEntity("u1", "alice", "Alice Agu",
                encoder.encode("securePass123"));
        userRepository.save(entity);

        var found = userRepository.findById("u1");
        assertTrue(found.isPresent(), "Entity should be persisted");
        assertEquals("alice", found.get().getUsername());
        assertEquals("Alice Agu", found.get().getDisplayName());
    }

    @Test
    void findByUsernameWorks() {
        var entity = new BotForgeUserEntity("u2", "bob", "Bob Builder",
                encoder.encode("password123"));
        userRepository.save(entity);

        var found = userRepository.findByUsername("bob");
        assertTrue(found.isPresent());
        assertEquals("u2", found.get().getId());
    }

    @Test
    void findByEmailWorks() {
        var entity = new BotForgeUserEntity("u3", "charlie", "Charlie Brown",
                encoder.encode("abcdef12"));
        entity.setEmail("charlie@example.com");
        userRepository.save(entity);

        var found = userRepository.findByEmail("charlie@example.com");
        assertTrue(found.isPresent());
        assertEquals("charlie", found.get().getUsername());
    }

    @Test
    void duplicateUsernameIsRejected() {
        var e1 = new BotForgeUserEntity("u4", "unique", "User 1", encoder.encode("pass1234"));
        userRepository.save(e1);

        var e2 = new BotForgeUserEntity("u5", "unique", "User 2", encoder.encode("pass5678"));
        assertThrows(Exception.class, () -> {
            userRepository.saveAndFlush(e2);
        }, "Duplicate username should be rejected by unique constraint");
    }

    @Test
    void passwordIsHashedCorrectly() {
        String rawPassword = "mySecurePassword";
        var entity = new BotForgeUserEntity("u6", "hashtest", "Hash Test",
                encoder.encode(rawPassword));
        userRepository.save(entity);

        var found = userRepository.findByUsername("hashtest").orElseThrow();
        assertTrue(encoder.matches(rawPassword, found.getPasswordHash()),
                "BCrypt hash should match the original password");
        assertNotEquals(rawPassword, found.getPasswordHash(),
                "Password should not be stored in plaintext");
    }

    @Test
    void toDomainUserConversion() {
        var entity = new BotForgeUserEntity("u7", "domain", "Domain User",
                encoder.encode("test1234"));
        var domainUser = entity.toDomainUser();

        assertEquals("u7", domainUser.getId());
        assertEquals("Domain User", domainUser.getDisplayName());
        assertEquals("domain", domainUser.getUsername());
    }
}
