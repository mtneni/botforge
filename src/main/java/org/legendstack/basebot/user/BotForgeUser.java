package org.legendstack.basebot.user;

import com.embabel.agent.api.identity.User;
import com.embabel.agent.api.reference.LlmReference;
import com.embabel.agent.filter.PropertyFilter;
import com.embabel.agent.rag.service.SearchOperations;
import com.embabel.agent.rag.tools.ToolishRag;
import org.legendstack.domain.common.Person;
import org.legendstack.basebot.rag.DocumentService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * User model for BotForge.
 * Implements NamedEntity so users can be referenced in DICE propositions.
 */
public class BotForgeUser implements User, Person {

    private final String id;
    private final String displayName;
    private final String username;
    private final String teamId;
    private final Set<String> roles;

    private String currentContextName;

    // Persona Studio overrides — set per session, read at respond time
    private volatile String personaOverride;
    private volatile String objectiveOverride;
    private volatile String behaviourOverride;
    private volatile String systemPromptOverride;
    private volatile String toolIdsOverride;

    public BotForgeUser(String id, String displayName, String username) {
        this(id, displayName, username, "default-team", Set.of("USER"));
    }

    public BotForgeUser(String id, String displayName, String username, String teamId, Set<String> roles) {
        this.id = id;
        this.displayName = displayName;
        this.username = username;
        this.teamId = teamId != null ? teamId : "default-team";
        this.roles = roles;
        this.currentContextName = "personal";
    }

    /**
     * The effective context is a unique identifier for the user's current context,
     * combining their user ID and the context name.
     */
    public String effectiveContext() {
        return id + "_" + currentContextName;
    }

    /**
     * Alias for effectiveContext() matching the Memory API convention.
     */
    public String currentContext() {
        return effectiveContext();
    }

    public void setCurrentContextName(String currentContextName) {
        this.currentContextName = currentContextName;
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public @NotNull String getDisplayName() {
        return displayName;
    }

    @Override
    public @NotNull String getUsername() {
        return username;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public String getTeamId() {
        return teamId;
    }

    @Override
    @Nullable
    public String getEmail() {
        return null;
    }

    public String getCurrentContextName() {
        return currentContextName;
    }

    // NamedEntity implementation

    @NotNull
    @Override
    public String getName() {
        return displayName;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "User: " + displayName;
    }

    @Nullable
    @Override
    public String getUri() {
        return null;
    }

    @NotNull
    @Override
    public Map<String, Object> getMetadata() {
        return Map.of();
    }

    @NotNull
    @Override
    public Set<String> labels() {
        return Set.of("BotForgeUser");
    }

    @NotNull
    @Override
    public String embeddableValue() {
        return getName() + ": " + getDescription();
    }

    @NotNull
    public LlmReference personalDocs(SearchOperations searchOperations) {
        return new ToolishRag(
                "user_docs",
                "User's own documents",
                searchOperations)
                .withMetadataFilter(
                        new PropertyFilter.Eq(
                                DocumentService.Context.CONTEXT_KEY,
                                effectiveContext()))
                .withUnfolding();
    }

    // Persona Studio overrides
    public String getPersonaOverride() {
        return personaOverride;
    }

    public void setPersonaOverride(String p) {
        this.personaOverride = p;
    }

    public String getObjectiveOverride() {
        return objectiveOverride;
    }

    public void setObjectiveOverride(String o) {
        this.objectiveOverride = o;
    }

    public String getBehaviourOverride() {
        return behaviourOverride;
    }

    public void setBehaviourOverride(String b) {
        this.behaviourOverride = b;
    }

    public String getSystemPromptOverride() {
        return systemPromptOverride;
    }

    public void setSystemPromptOverride(String s) {
        this.systemPromptOverride = s;
    }

    public String getToolIdsOverride() {
        return toolIdsOverride;
    }

    public void setToolIdsOverride(String t) {
        this.toolIdsOverride = t;
    }
}
