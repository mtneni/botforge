package org.legendstack.basebot.api;

import com.embabel.agent.rag.model.NamedEntityData;
import com.embabel.agent.rag.service.NamedEntityDataRepository;
import org.legendstack.basebot.user.BotForgeUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST endpoints for entities in the knowledge graph.
 */
@RestController
@RequestMapping("/api/entities")
public class EntityController {

    private final NamedEntityDataRepository entityRepository;
    private final BotForgeUserService userService;

    public EntityController(NamedEntityDataRepository entityRepository, BotForgeUserService userService) {
        this.entityRepository = entityRepository;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> listEntities(@RequestParam(required = false) String contextId) {
        var user = userService.getAuthenticatedUser();
        var effectiveContextId = contextId != null ? contextId : user.effectiveContext();

        try {
            var entities = entityRepository.withContextScope(effectiveContextId)
                    .findByLabel(NamedEntityData.ENTITY_LABEL);

            var result = entities.stream()
                    .sorted(Comparator.comparing(NamedEntityData::getName, String.CASE_INSENSITIVE_ORDER))
                    .map(entity -> {
                        var m = new LinkedHashMap<String, Object>();
                        m.put("id", entity.getId());
                        m.put("name", entity.getName());
                        m.put("description", entity.getDescription());
                        m.put("labels", entity.labels());
                        m.put("properties", entity.getProperties());
                        return m;
                    })
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "entities", result,
                    "count", entities.size()));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("entities", List.of(), "count", 0));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEntity(@PathVariable String id) {
        var entity = entityRepository.findEntityById(id);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "id", entity.getId(),
                "name", entity.getName(),
                "description", entity.getDescription(),
                "labels", entity.labels(),
                "metadata", entity.getMetadata()));
    }
}
