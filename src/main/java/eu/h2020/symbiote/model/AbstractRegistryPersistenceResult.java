package eu.h2020.symbiote.model;

/**
 * Created by mateuszl on 07.08.2017.
 */
public abstract class AbstractRegistryPersistenceResult<T> {

    private int status;
    private String message;
    private T persistenceObject;

    public AbstractRegistryPersistenceResult() {
    }

    public AbstractRegistryPersistenceResult(int status, String message, T persistenceObject) {
        this.status = status;
        this.message = message;
        this.persistenceObject = persistenceObject;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getPersistenceObject() {
        return persistenceObject;
    }

    public void setPersistenceObject(T persistenceObject) {
        this.persistenceObject = persistenceObject;
    }
}
