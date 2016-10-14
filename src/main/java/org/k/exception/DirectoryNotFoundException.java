package org.k.exception;

@SuppressWarnings("unused")
public class DirectoryNotFoundException extends RuntimeException {
    public DirectoryNotFoundException() {
    }

    public DirectoryNotFoundException(String message) {
        super(message);
    }

    public DirectoryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public DirectoryNotFoundException(Throwable cause) {
        super(cause);
    }

    public DirectoryNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
