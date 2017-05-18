package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.model.RegistryPlatform;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Registry MongoDB Persistence layer for RegistryPlatform objects
 *
 * Created by mateuszl
 */
@Repository
public interface RegistryPlatformRepository extends MongoRepository<RegistryPlatform, String>{

}
