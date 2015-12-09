package io.askcuix.push.exception;

/**
 * Created by Chris on 15/12/9.
 */
public class PushException extends RuntimeException {

    public PushException() {
        super();
    }

    public PushException(String message) {
        super(message);
    }

    public PushException(Throwable cause) {
        super(cause);
    }

    public PushException(String message, Throwable cause) {
        super(message, cause);
    }

}
