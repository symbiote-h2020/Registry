package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.core.model.Federation;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Created by mateuszl on 22.08.2017.
 */
public interface FederationRepository extends MongoRepository<Federation, String> {

    Federation getByMember(String member);
}