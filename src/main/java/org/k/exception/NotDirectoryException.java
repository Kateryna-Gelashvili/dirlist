package org.k.exception;

@SuppressWarnings("unused")
public class NotDirectoryException extends RuntimeException {
    public NotDirectoryException() {
    }

    public NotDirectoryException(String message) {
        super(message);
    }

    public NotDirectoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotDirectoryException(Throwable cause) {
        super(cause);
    }

    public NotDirectoryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
