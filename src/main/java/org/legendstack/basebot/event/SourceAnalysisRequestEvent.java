package org.legendstack.basebot.event;

import com.embabel.chat.Message;
import com.embabel.dice.incremental.IncrementalSource;
import org.legendstack.basebot.user.BotForgeUser;
import org.springframework.context.ApplicationEvent;

public abstract class SourceAnalysisRequestEvent extends ApplicationEvent {

    public final BotForgeUser user;

    public SourceAnalysisRequestEvent(
            Object source,
            BotForgeUser user
    ) {
        super(source);
        this.user = user;
    }

    public abstract IncrementalSource<Message> incrementalSource();
}
