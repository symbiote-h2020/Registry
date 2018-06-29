package eu.h2020.symbiote.utils.Exceptions;

/**
 * Created by mateuszl on 29.06.2018.
 */
public class ExceptionWithStatusCode extends Exception {
    private int statusCode;

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public ExceptionWithStatusCode(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}

//todo think about it