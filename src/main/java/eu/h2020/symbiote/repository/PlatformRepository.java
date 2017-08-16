package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.core.model.Platform;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Registry MongoDB Persistence layer for Platform objects
 *
 * Created by mateuszl
 */
@Repository
public interface PlatformRepository extends MongoRepository<Platform, String> {

    List<Platform> findByInterworkingSericeUrl(String url);
}
