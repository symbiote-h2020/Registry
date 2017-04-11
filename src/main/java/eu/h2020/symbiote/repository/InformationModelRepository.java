package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.model.InformationModel;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Created by mateuszl on 14.03.2017.
 */
public interface InformationModelRepository extends MongoRepository<InformationModel, String> {
}
