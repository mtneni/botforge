package org.legendstack.basebot.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity for persisting BotForge users in PostgreSQL.
 * Used by {@link JpaBotForgeUserService} in production profiles.
 */
@Entity
@Table(name = "botforge_users")
public class BotForgeUserEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(nullable = false)
    private String passwordHash;

    @Column(length = 256)
    private String email;

    @Column(length = 32)
    private String role = "USER";

    @Column(name = "team_id", length = 64)
    private String teamId;

    protected BotForgeUserEntity() {
        // JPA no-arg constructor
    }

    public BotForgeUserEntity(String id, String username, String displayName, String passwordHash) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    /**
     * Convert this JPA entity to the domain model used throughout the application.
     */
    public BotForgeUser toDomainUser() {
        return new BotForgeUser(id, displayName, username);
    }
}
