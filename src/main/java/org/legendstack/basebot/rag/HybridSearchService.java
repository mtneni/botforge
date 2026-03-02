package org.legendstack.basebot.rag;

import com.embabel.dice.proposition.Proposition;
import org.legendstack.basebot.proposition.persistence.DrivinePropositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Hybrid search service that combines vector similarity search and keyword
 * (full-text index) search, merging results with Reciprocal Rank Fusion (RRF).
 *
 * <p>
 * RRF formula: score(d) = Σ 1/(k + rank_i) where k=60 (standard constant).
 * This produces a unified ranking that benefits from both semantic
 * understanding
 * and exact keyword matching.
 * </p>
 */
@Service
public class HybridSearchService {

    private static final Logger logger = LoggerFactory.getLogger(HybridSearchService.class);
    private static final int RRF_K = 60;

    private final DrivinePropositionRepository propositionRepository;

    public HybridSearchService(DrivinePropositionRepository propositionRepository) {
        this.propositionRepository = propositionRepository;
    }

    /**
     * Perform hybrid search: vector similarity + keyword full-text, merged with
     * RRF.
     *
     * @param query  the user's search query
     * @param teamId the team context for multi-tenant isolation
     * @param topK   the number of results to return
     * @return ranked list of propositions, fused from both search strategies
     */
    public List<Proposition> hybridSearch(String query, String teamId, int topK) {
        int fetchLimit = topK * 3;

        // --- Vector search (via embedding similarity) ---
        List<String> vectorIds;
        try {
            vectorIds = propositionRepository.findSimilarIds(query, teamId, fetchLimit);
            logger.debug("Vector search returned {} results for: {}", vectorIds.size(), query);
        } catch (Exception e) {
            logger.warn("Vector search failed, keyword only: {}", e.getMessage());
            vectorIds = List.of();
        }

        // --- Keyword search (via Neo4j full-text index) ---
        List<String> keywordIds;
        try {
            keywordIds = propositionRepository.findByKeyword(query, teamId, fetchLimit);
            logger.debug("Keyword search returned {} results for: {}", keywordIds.size(), query);
        } catch (Exception e) {
            logger.warn("Keyword search failed: {}", e.getMessage());
            keywordIds = List.of();
        }

        // --- Reciprocal Rank Fusion ---
        Map<String, Double> rrfScores = new LinkedHashMap<>();

        for (int i = 0; i < vectorIds.size(); i++) {
            rrfScores.merge(vectorIds.get(i), 1.0 / (RRF_K + i + 1), (a, b) -> a + b);
        }
        for (int i = 0; i < keywordIds.size(); i++) {
            rrfScores.merge(keywordIds.get(i), 1.0 / (RRF_K + i + 1), (a, b) -> a + b);
        }

        List<String> fusedIds = rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        logger.info("Hybrid search: {} vector + {} keyword → {} fused for: {}",
                vectorIds.size(), keywordIds.size(), fusedIds.size(), query);

        return fusedIds.stream()
                .map(propositionRepository::findById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
