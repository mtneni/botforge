package org.legendstack.basebot.api;

import com.embabel.agent.core.DataDictionary;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST endpoint for the DICE data dictionary schema.
 */
@RestController
@RequestMapping("/api/schema")
public class SchemaController {

    private final DataDictionary dataDictionary;

    public SchemaController(DataDictionary dataDictionary) {
        this.dataDictionary = dataDictionary;
    }

    @GetMapping
    public ResponseEntity<?> getSchema() {
        var types = dataDictionary.getDomainTypes().stream()
                .sorted((a, b) -> a.getOwnLabel().compareToIgnoreCase(b.getOwnLabel()))
                .map(type -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("label", type.getOwnLabel());
                    m.put("description", type.getDescription());

                    var props = type.getValues().stream()
                            .map(prop -> Map.of("name", prop.getName()))
                            .toList();
                    m.put("properties", props);

                    var rels = type.getRelationships().stream()
                            .map(rel -> Map.of(
                                    "name", rel.getName(),
                                    "targetType", rel.getType().getOwnLabel()))
                            .toList();
                    m.put("relationships", rels);

                    return m;
                })
                .toList();

        return ResponseEntity.ok(Map.of(
                "types", types,
                "count", types.size()));
    }
}
