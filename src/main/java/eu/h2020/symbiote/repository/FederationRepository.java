package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.core.model.Federation;
import eu.h2020.symbiote.core.model.Platform;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Created by mateuszl on 22.08.2017.
 */
public interface FederationRepository extends MongoRepository<Federation, String> {

    List<Federation> findByMemberPlatform(Platform platform);
}