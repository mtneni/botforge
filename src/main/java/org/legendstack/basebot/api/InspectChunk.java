package org.legendstack.basebot.api;

import com.embabel.agent.rag.model.Chunk;
import java.util.Arrays;

public class InspectChunk {
    public static void main(String[] args) {
        System.out.println("Methods in Chunk class:");
        Arrays.stream(Chunk.class.getMethods())
                .forEach(m -> System.out.println(m.getName() + " -> " + m.getReturnType().getSimpleName()));
    }
}
