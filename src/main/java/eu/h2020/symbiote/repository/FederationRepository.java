package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.core.model.Federation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Registry MongoDB Persistence layer for Federation objects
 *
 * Created by mateuszl on 22.08.2017.
 */
@Repository
public interface FederationRepository extends MongoRepository<Federation, String> {
    //List<Federation> findByMemberPlatformId(String platformId); // TODO: 24.08.2017 QUERY
}