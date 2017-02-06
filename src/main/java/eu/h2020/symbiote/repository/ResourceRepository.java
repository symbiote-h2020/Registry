package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.model.Resource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Registry MongoDB Persistence layer for Resource objects
 */
@Repository
public interface ResourceRepository extends MongoRepository<Resource, String> {
}
