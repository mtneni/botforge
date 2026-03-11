package org.legendstack.basebot;

import com.embabel.agent.api.tool.Tool;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ToolInspector {
    public static void main(String[] args) {
        System.out.println("Methods in Tool class:");
        for (Method m : Tool.class.getMethods()) {
            System.out.println(m.getName() + " -> " + m.getReturnType().getSimpleName());
        }
    }
}
