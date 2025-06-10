package app.model.entities.common;

public class ValidException extends Exception {
    public ValidException(String message) {
        super(message);
    }

    public ValidException(String message, Throwable cause) {
        super(message, cause);
    }
}
