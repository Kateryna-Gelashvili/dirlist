package org.k.controller;

import org.k.exception.DirectoryNotFoundException;
import org.k.exception.RangeNotSatisfiableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


@ControllerAdvice
public class DirlistExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(DirlistExceptionHandler.class);

    @ExceptionHandler(DirectoryNotFoundException.class)
    public ResponseEntity<String> handleDirectoryNotFound(DirectoryNotFoundException e) {
        logger.error(e.getMessage(), e);
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RangeNotSatisfiableException.class)
    public ResponseEntity<String> handleRangeNotSatisfiable(RangeNotSatisfiableException e) {
        logger.warn(e.getMessage());
        return new ResponseEntity<>(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
    }
}
