package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.model.Location;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Registry MongoDB Persistence layer for Location objects
 */
@Repository
public interface LocationRepository extends MongoRepository<Location,String>{
}