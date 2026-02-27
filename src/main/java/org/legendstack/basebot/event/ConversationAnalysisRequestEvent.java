package org.legendstack.basebot.event;

import com.embabel.chat.Conversation;
import com.embabel.chat.Message;
import com.embabel.dice.incremental.ConversationSource;
import com.embabel.dice.incremental.IncrementalSource;
import org.legendstack.basebot.user.BotForgeUser;

/**
 * Event published after a conversation exchange (user message + assistant response).
 * Used to trigger async proposition extraction.
 */
public class ConversationAnalysisRequestEvent extends SourceAnalysisRequestEvent {
    
    public final Conversation conversation;

    public ConversationAnalysisRequestEvent(
            Object source,
            BotForgeUser user,
            Conversation conversation) {
        super(source, user);
        this.conversation = conversation;
    }

    @Override
    public IncrementalSource<Message> incrementalSource() {
        return new ConversationSource(conversation);
    }
}
