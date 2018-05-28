package eu.h2020.symbiote.model.persistenceResults;

import eu.h2020.symbiote.model.mim.Platform;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * Created by mateuszl on 07.08.2017.
 */
public class PlatformPersistenceResult extends AbstractPersistenceResult<Platform> {

    public PlatformPersistenceResult() {
    }

    public PlatformPersistenceResult(int status, String message, Platform persistenceObject) {
        super(status, message, persistenceObject);
    }

    public void setPlatform(Platform platform){
        super.setPersistenceObject(platform);
    }

    public Platform getPlatform(){
        return super.getPersistenceObject();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
