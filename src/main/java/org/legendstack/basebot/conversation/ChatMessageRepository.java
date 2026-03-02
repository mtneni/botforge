package org.legendstack.basebot.conversation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, String> {

    /**
     * Full-text search across message content for a specific user's conversations.
     */
    @Query("SELECT m FROM ChatMessageEntity m JOIN m.conversation c " +
            "WHERE c.userId = :userId AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY m.timestamp DESC")
    Page<ChatMessageEntity> searchByContent(@Param("userId") String userId,
            @Param("query") String query,
            Pageable pageable);

    /**
     * Find all messages for a conversation ordered by timestamp.
     */
    List<ChatMessageEntity> findByConversationIdOrderByTimestampAsc(String conversationId);
}
