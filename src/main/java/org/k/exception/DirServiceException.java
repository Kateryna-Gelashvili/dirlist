package org.k.exception;

public class DirServiceException extends RuntimeException {
    public DirServiceException() {
    }

    public DirServiceException(String message) {
        super(message);
    }
}
