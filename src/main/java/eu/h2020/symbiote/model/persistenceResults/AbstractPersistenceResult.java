package eu.h2020.symbiote.model.persistenceResults;

/**
 * Created by mateuszl on 07.08.2017.
 */
public abstract class AbstractPersistenceResult<T> {

    private int status;
    private String message;
    private T persistenceObject;

    public AbstractPersistenceResult() {
    }

    public AbstractPersistenceResult(int status, String message, T persistenceObject) {
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
