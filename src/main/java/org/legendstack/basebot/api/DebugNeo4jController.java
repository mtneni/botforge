package org.legendstack.basebot.api;

import org.drivine.manager.PersistenceManager;
import org.drivine.query.QuerySpecification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug-neo4j")
public class DebugNeo4jController {

    private final PersistenceManager persistenceManager;

    public DebugNeo4jController(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @GetMapping
    public ResponseEntity<?> debug() {
        String cypher = "MATCH p=(n)-[*1..3]->(c:Chunk) RETURN extract(r in relationships(p) | type(r)) as rels LIMIT 1";
        try {
            List<Map> rows = (List<Map>) (List) persistenceManager.query(
                    QuerySpecification.withStatement(cypher).transform(Map.class));
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
