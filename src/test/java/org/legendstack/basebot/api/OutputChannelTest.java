package org.legendstack.basebot.api;

import com.embabel.agent.api.channel.OutputChannelEvent;
import com.embabel.chat.AssistantMessage;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.Arrays;

public class OutputChannelTest {

    @Test
    public void inspectEvents() {
        System.out.println("--- Inspecting Chatbot ---");
        printMethods(com.embabel.chat.Chatbot.class);

        System.out.println("\n--- Inspecting AssistantMessage ---");
        printMethods(AssistantMessage.class);

        System.out.println("\n--- Inspecting OutputChannelEvent ---");
        printMethods(OutputChannelEvent.class);

        String[] likely = {
                "com.embabel.agent.api.channel.IncrementalOutputChannelEvent",
                "com.embabel.agent.api.channel.TokenOutputChannelEvent",
                "com.embabel.agent.api.channel.ProgressOutputChannelEvent"
        };

        for (String c : likely) {
            try {
                Class<?> clazz = Class.forName(c);
                System.out.println("\nFound: " + c);
                printMethods(clazz);
            } catch (ClassNotFoundException e) {
                System.out.println("\nNot found: " + c);
            }
        }
    }

    private void printMethods(Class<?> clazz) {
        Arrays.stream(clazz.getMethods())
                .map(Method::getName)
                .distinct()
                .sorted()
                .forEach(m -> System.out.println("  " + m));
    }
}
