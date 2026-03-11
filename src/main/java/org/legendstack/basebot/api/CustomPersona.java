package org.legendstack.basebot.api;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "custom_personas")
public class CustomPersona {

    @Id
    private String id;

    private String userId;
    private String displayName;

    @Column(length = 2000)
    private String objective;

    private String behaviour;
    private String description;
    private String icon;

    @Column(columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "tool_ids")
    private String toolIds;

    public CustomPersona() {
    }

    public CustomPersona(String id, String userId, String displayName, String objective, String behaviour,
            String description, String icon, String systemPrompt, String toolIds) {
        this.id = id;
        this.userId = userId;
        this.displayName = displayName;
        this.objective = objective;
        this.behaviour = behaviour;
        this.description = description;
        this.icon = icon;
        this.systemPrompt = systemPrompt;
        this.toolIds = toolIds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getBehaviour() {
        return behaviour;
    }

    public void setBehaviour(String behaviour) {
        this.behaviour = behaviour;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getToolIds() {
        return toolIds;
    }

    public void setToolIds(String toolIds) {
        this.toolIds = toolIds;
    }
}
