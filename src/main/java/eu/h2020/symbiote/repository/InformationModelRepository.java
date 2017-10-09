package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.core.model.InformationModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Registry MongoDB Persistence layer for Information Model objects
 *
 * Created by mateuszl on 08.08.2017.
 */
@Repository
public interface InformationModelRepository extends MongoRepository<InformationModel, String> {
}
