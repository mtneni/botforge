package org.legendstack.basebot.api;

import com.embabel.agent.rag.service.Cluster;
import com.embabel.dice.proposition.Proposition;
import com.embabel.dice.proposition.PropositionQuery;
import org.legendstack.basebot.event.ConversationAnalysisRequestEvent;
import org.legendstack.basebot.proposition.extraction.IncrementalPropositionExtraction;
import org.legendstack.basebot.proposition.persistence.DrivinePropositionRepository;
import org.legendstack.basebot.user.BotForgeUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * REST endpoints for memory (propositions), DICE extraction, and clustering.
 */
@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private static final Logger logger = LoggerFactory.getLogger(MemoryController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final DrivinePropositionRepository propositionRepository;
    private final IncrementalPropositionExtraction propositionExtraction;
    private final BotForgeUserService userService;
    private final ChatSessionManager sessionManager;

    public MemoryController(DrivinePropositionRepository propositionRepository,
            IncrementalPropositionExtraction propositionExtraction,
            BotForgeUserService userService,
            ChatSessionManager sessionManager) {
        this.propositionRepository = propositionRepository;
        this.propositionExtraction = propositionExtraction;
        this.userService = userService;
        this.sessionManager = sessionManager;
    }

    @GetMapping("/propositions")
    public ResponseEntity<?> getPropositions(@RequestParam(required = false) String contextId) {
        var propositions = contextId != null
                ? propositionRepository.findByContextIdValue(contextId)
                : propositionRepository.findAll();

        var result = propositions.stream()
                .sorted(Comparator.comparing(Proposition::getCreated).reversed())
                .map(this::toPropositionMap)
                .toList();

        return ResponseEntity.ok(Map.of(
                "propositions", result,
                "count", propositions.size()));
    }

    @GetMapping("/clusters")
    public ResponseEntity<?> getClusters(@RequestParam(required = false) String contextId) {
        var query = PropositionQuery.againstContext(contextId);
        List<Cluster<Proposition>> clusters = propositionRepository.findClusters(0.7, 10, query);

        var clusteredIds = new HashSet<String>();
        for (var cluster : clusters) {
            clusteredIds.add(cluster.getAnchor().getId());
            for (var sim : cluster.getSimilar()) {
                clusteredIds.add(sim.getMatch().getId());
            }
        }

        var allPropositions = contextId != null
                ? propositionRepository.findByContextIdValue(contextId)
                : propositionRepository.findAll();

        var clusterResult = clusters.stream().map(cluster -> {
            var anchor = toPropositionMap(cluster.getAnchor());
            var similar = cluster.getSimilar().stream()
                    .map(sim -> Map.of(
                            "proposition", toPropositionMap(sim.getMatch()),
                            "score", Math.round(sim.getScore() * 100)))
                    .toList();
            return Map.of(
                    "anchor", anchor,
                    "similar", similar,
                    "size", cluster.getSimilar().size() + 1);
        }).toList();

        var unclustered = allPropositions.stream()
                .filter(p -> !clusteredIds.contains(p.getId()))
                .sorted(Comparator.comparing(Proposition::getCreated).reversed())
                .map(this::toPropositionMap)
                .toList();

        return ResponseEntity.ok(Map.of(
                "clusters", clusterResult,
                "unclustered", unclustered,
                "totalCount", allPropositions.size(),
                "clusterCount", clusters.size()));
    }

    @PostMapping("/learn")
    public ResponseEntity<?> learn(@RequestParam("file") MultipartFile file) {
        var user = userService.getAuthenticatedUser();
        try {
            propositionExtraction.rememberFile(file.getInputStream(), file.getOriginalFilename(), user);
            return ResponseEntity.ok(Map.of("message", "Learning from: " + file.getOriginalFilename()));
        } catch (Exception e) {
            logger.error("Failed to learn from file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/analyze/{conversationId}")
    public ResponseEntity<?> analyze(@PathVariable String conversationId) {
        var user = userService.getAuthenticatedUser();
        var sessionData = sessionManager.get(conversationId);
        if (sessionData != null) {
            var conversation = sessionData.chatSession().getConversation();
            propositionExtraction.extractPropositions(
                    new ConversationAnalysisRequestEvent(this, user, conversation));
            return ResponseEntity.ok(Map.of("message", "Analysis triggered for conversation: " + conversationId));
        }
        return ResponseEntity.badRequest()
                .body(Map.of("error", "No active chat session for conversation: " + conversationId));
    }

    @DeleteMapping("/propositions/{id}")
    public ResponseEntity<?> deleteProposition(@PathVariable String id) {
        propositionRepository.delete(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    @DeleteMapping("/propositions")
    public ResponseEntity<?> clearAll(@RequestParam String contextId) {
        propositionRepository.clearByContext(contextId);
        return ResponseEntity.ok(Map.of("message", "Cleared all memories for context: " + contextId));
    }

    private Map<String, Object> toPropositionMap(Proposition prop) {
        var result = new LinkedHashMap<String, Object>();
        result.put("id", prop.getId());
        result.put("text", prop.getText());
        result.put("confidence", Math.round(prop.getConfidence() * 100));
        result.put("created", TIME_FORMATTER.format(prop.getCreated()));

        var mentions = prop.getMentions().stream()
                .map(m -> {
                    var mm = new LinkedHashMap<String, Object>();
                    mm.put("span", m.getSpan());
                    mm.put("type", m.getType());
                    mm.put("resolvedId", m.getResolvedId());
                    return mm;
                })
                .toList();
        result.put("mentions", mentions);

        return result;
    }
}
