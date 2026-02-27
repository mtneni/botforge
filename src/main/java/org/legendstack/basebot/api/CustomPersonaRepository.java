package org.legendstack.basebot.api;

import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface CustomPersonaRepository extends CrudRepository<CustomPersona, String> {
    List<CustomPersona> findByUserId(String userId);
}
