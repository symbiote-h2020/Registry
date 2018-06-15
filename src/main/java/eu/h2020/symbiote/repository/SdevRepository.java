package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.cloud.model.ssp.SspRegInfo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for internal SSP Sdev (SspRegInfo) objects.
 * <p>
 * Created by mateuszl on 25.05.2018.
 */
@Repository
public interface SdevRepository extends MongoRepository<SspRegInfo, String> {
}
