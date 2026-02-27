package org.legendstack.basebot.api;

import com.embabel.agent.api.channel.OutputChannelEvent;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ReflectTool {
    public static void main(String[] args) {
        try {
            System.out.println("Inspecting OutputChannelEvent:");
            printInfo(OutputChannelEvent.class);

            // Try to load likely candidates
            String[] candidates = {
                    "com.embabel.agent.api.channel.IncrementalOutputChannelEvent",
                    "com.embabel.agent.api.channel.TokenOutputChannelEvent",
                    "com.embabel.agent.api.channel.StreamingOutputChannelEvent",
                    "com.embabel.agent.api.channel.PartialOutputChannelEvent"
            };

            for (String c : candidates) {
                try {
                    Class<?> clazz = Class.forName(c);
                    System.out.println("\nFound Candidate: " + c);
                    printInfo(clazz);
                } catch (ClassNotFoundException e) {
                    // ignore
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printInfo(Class<?> clazz) {
        System.out.println("Class: " + clazz.getName());
        System.out.println("Methods:");
        Arrays.stream(clazz.getMethods())
                .map(Method::getName)
                .distinct()
                .forEach(m -> System.out.println("  - " + m));
    }
}
