package eu.h2020.symbiote.model;

import eu.h2020.symbiote.core.model.Platform;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * Created by mateuszl on 07.08.2017.
 */
public class PlatformPersistenceResult extends AbstractRegistryPersistenceResult<Platform>{

    public PlatformPersistenceResult() {
    }

    public PlatformPersistenceResult(int status, String message, Platform persistenceObject) {
        super(status, message, persistenceObject);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
