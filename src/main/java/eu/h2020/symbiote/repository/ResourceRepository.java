package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.core.model.internal.CoreResource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Registry MongoDB Persistence layer for Resource objects
 *
 * Created by mateuszl
 */
@Repository
public interface ResourceRepository extends MongoRepository<CoreResource, String> {

    List<CoreResource> findByPlatformId(String platformId);
}
