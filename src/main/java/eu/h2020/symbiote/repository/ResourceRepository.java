package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.model.Resource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by mateuszl on 17.01.2017.
 */
@Repository
public interface ResourceRepository extends MongoRepository<Resource, String> {
}
