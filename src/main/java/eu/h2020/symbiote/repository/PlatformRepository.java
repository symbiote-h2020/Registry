package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.model.RegistryPlatform;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Registry MongoDB Persistence layer for Platform objects
 *
 * Created by mateuszl
 */
@Repository
public interface PlatformRepository extends MongoRepository<RegistryPlatform, String>{

}
