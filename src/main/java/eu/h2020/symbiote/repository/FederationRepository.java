package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.model.mim.Federation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Registry MongoDB Persistence layer for Federation objects
 * <p>
 * Created by mateuszl on 22.08.2017.
 */
@Repository
public interface FederationRepository extends MongoRepository<Federation, String> {
    List<Federation> findByMembersPlatformId(String platformId); // TODO: 24.08.2017 QUERY
}