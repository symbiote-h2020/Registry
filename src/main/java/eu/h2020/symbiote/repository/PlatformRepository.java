package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.model.Platform;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Created by mateuszl on 09.01.2017.
 */
public interface PlatformRepository extends MongoRepository<Platform, String>{
}
