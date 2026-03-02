package org.legendstack.basebot.conversation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {

    List<Conversation> findByUserIdOrderByUpdatedAtDesc(String userId);

    /**
     * Search conversations by title for a specific user.
     */
    List<Conversation> findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(
            String userId, String title);

    /**
     * Count conversations per user.
     */
    long countByUserId(String userId);

    /**
     * Count all conversations.
     */
    @Query("SELECT COUNT(DISTINCT c.userId) FROM Conversation c")
    long countDistinctUsers();
}
