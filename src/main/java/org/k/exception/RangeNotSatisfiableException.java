package org.k.exception;

public class RangeNotSatisfiableException extends RuntimeException {
    public RangeNotSatisfiableException(String range) {
        super("Range not satisfiable: " + range);
    }
}
