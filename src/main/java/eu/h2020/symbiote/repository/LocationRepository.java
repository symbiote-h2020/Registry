package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.model.Location;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by mateuszl on 19.01.2017.
 */
@Repository
public interface LocationRepository extends MongoRepository<Location,String>{
}