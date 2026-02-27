package org.legendstack.basebot;

import com.embabel.chat.AssistantMessage;
import java.lang.reflect.Method;

public class TestMethods {
    public static void main(String[] args) {
        for (Method m : AssistantMessage.class.getMethods()) {
            System.err.println("METHOD: " + m.getName() + " -> " + m.getReturnType().getName());
        }
    }
}
