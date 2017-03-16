package eu.h2020.symbiote.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Created by mateuszl on 14.03.2017.
 */
public interface InterworkingServiceRepository extends MongoRepository<InterworkingService, String> {

    List<InterworkingService> findByUrl(String url);
}
