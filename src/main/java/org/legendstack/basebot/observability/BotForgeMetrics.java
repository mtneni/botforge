package org.legendstack.basebot.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Centralizes all custom business metrics for BotForge.
 * Avoids scattering metric names across the codebase — all metrics
 * are defined, documented, and incremented through this single class.
 */
@Component
public class BotForgeMetrics {

    // --- Chat Metrics ---
    private final Counter chatMessagesTotal;
    private final Counter chatResponsesTotal;
    private final Counter chatErrorsTotal;
    private final Timer chatResponseTime;

    // --- Cache Metrics ---
    private final Counter cacheHits;
    private final Counter cacheMisses;

    // --- Persona Metrics ---
    private final Counter personaSwitches;

    // --- Memory Metrics ---
    private final Counter propositionsExtracted;

    public BotForgeMetrics(MeterRegistry registry) {
        this.chatMessagesTotal = Counter.builder("botforge.chat.messages.total")
                .description("Total chat messages received from users")
                .register(registry);

        this.chatResponsesTotal = Counter.builder("botforge.chat.responses.total")
                .description("Total chat responses sent to users")
                .register(registry);

        this.chatErrorsTotal = Counter.builder("botforge.chat.errors.total")
                .description("Total chat processing errors")
                .register(registry);

        this.chatResponseTime = Timer.builder("botforge.chat.response.time")
                .description("Time to generate a chat response")
                .register(registry);

        this.cacheHits = Counter.builder("botforge.cache.hits")
                .description("Semantic cache hits")
                .register(registry);

        this.cacheMisses = Counter.builder("botforge.cache.misses")
                .description("Semantic cache misses")
                .register(registry);

        this.personaSwitches = Counter.builder("botforge.persona.switches")
                .description("Total persona switches")
                .register(registry);

        this.propositionsExtracted = Counter.builder("botforge.memory.propositions.extracted")
                .description("Total propositions extracted from conversations")
                .register(registry);
    }

    public void recordChatMessage() {
        chatMessagesTotal.increment();
    }

    public void recordChatResponse() {
        chatResponsesTotal.increment();
    }

    public void recordChatError() {
        chatErrorsTotal.increment();
    }

    public Timer.Sample startChatTimer() {
        return Timer.start();
    }

    public void stopChatTimer(Timer.Sample sample) {
        sample.stop(chatResponseTime);
    }

    public void recordCacheHit() {
        cacheHits.increment();
    }

    public void recordCacheMiss() {
        cacheMisses.increment();
    }

    public void recordPersonaSwitch() {
        personaSwitches.increment();
    }

    public void recordPropositionExtracted() {
        propositionsExtracted.increment();
    }
}
