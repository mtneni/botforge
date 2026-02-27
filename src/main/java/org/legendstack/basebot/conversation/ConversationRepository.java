package org.legendstack.basebot.conversation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(String userId);
}
