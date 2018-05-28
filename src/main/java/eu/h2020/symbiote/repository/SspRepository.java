package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.model.mim.SmartSpace;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for SmartSpace objects.
 * <p>
 * Created by mateuszl on 25.05.2018.
 */
@Repository
public interface SspRepository extends MongoRepository<SmartSpace, String> {
}
