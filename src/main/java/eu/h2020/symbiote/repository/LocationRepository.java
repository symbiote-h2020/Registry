package eu.h2020.symbiote.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Registry MongoDB Persistence layer for Location objects
 *
 * Created by mateuszl
 */
@Repository
public interface LocationRepository extends MongoRepository<Location,String>{
}