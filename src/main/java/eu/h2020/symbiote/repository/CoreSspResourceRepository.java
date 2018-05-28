package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.model.CoreSspResource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for internal CoreSspResource objects.
 * <p>
 * Created by mateuszl on 25.05.2018.
 */
@Repository
public interface CoreSspResourceRepository extends MongoRepository<CoreSspResource, String> {
}
