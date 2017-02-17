package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.model.Resource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Registry MongoDB Persistence layer for Resource objects
 *
 * Created by mateuszl
 */
@Repository
public interface ResourceRepository extends MongoRepository<Resource, String> {

    public List<Resource> findByPlatformId(String platformId);
}
