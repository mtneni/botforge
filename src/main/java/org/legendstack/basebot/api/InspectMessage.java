package org.legendstack.basebot.api;

import com.embabel.chat.AssistantMessage;
import com.embabel.agent.api.channel.MessageOutputChannelEvent;
import java.lang.reflect.Method;
import java.util.Arrays;

public class InspectMessage {
    public static void main(String[] args) {
        System.out.println("Methods in AssistantMessage:");
        Arrays.stream(AssistantMessage.class.getMethods())
                .map(Method::getName)
                .distinct()
                .forEach(System.out::println);

        System.out.println("\nMethods in MessageOutputChannelEvent:");
        Arrays.stream(MessageOutputChannelEvent.class.getMethods())
                .map(Method::getName)
                .distinct()
                .forEach(System.out::println);
    }
}
