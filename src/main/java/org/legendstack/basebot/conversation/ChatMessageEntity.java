package org.legendstack.basebot.conversation;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "chat_messages")
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(nullable = false)
    private String role; // user, assistant, system

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT")
    private String citations; // JSON string of citations

    @Column(nullable = false)
    private Instant timestamp;

    public ChatMessageEntity() {
    }

    public ChatMessageEntity(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCitations() {
        return citations;
    }

    public void setCitations(String citations) {
        this.citations = citations;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
