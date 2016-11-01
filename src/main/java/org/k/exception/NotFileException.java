package org.k.exception;

@SuppressWarnings("unused")
public class NotFileException extends RuntimeException {
    public NotFileException() {
    }

    public NotFileException(String message) {
        super(message);
    }

    public NotFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFileException(Throwable cause) {
        super(cause);
    }

    public NotFileException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
