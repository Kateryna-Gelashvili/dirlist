package org.k.controller;

import org.k.dto.ErrorDto;
import org.k.exception.DirectoryNotFoundException;
import org.k.exception.FileNotFoundException;
import org.k.exception.MaxDirectoryDownloadSizeExceededException;
import org.k.exception.NotDirectoryException;
import org.k.exception.NotFileException;
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

    @ExceptionHandler(RangeNotSatisfiableException.class)
    public ResponseEntity<String> handleRangeNotSatisfiableException(RangeNotSatisfiableException e) {
        logger.warn(e.getMessage());
        return new ResponseEntity<>(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
    }

    @ExceptionHandler(NotDirectoryException.class)
    public ResponseEntity<ErrorDto> handleNotDirectoryException(NotDirectoryException e) {
        logger.warn(e.getMessage());
        return new ResponseEntity<>(
                new ErrorDto(1001, "Requested resource is not a directory."),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DirectoryNotFoundException.class)
    public ResponseEntity<ErrorDto> handleDirectoryNotFoundException(DirectoryNotFoundException e) {
        logger.warn(e.getMessage());
        return new ResponseEntity<>(
                new ErrorDto(1002, "Requested directory could not be found."),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NotFileException.class)
    public ResponseEntity<ErrorDto> handleNotFileException(NotFileException e) {
        logger.warn(e.getMessage());
        return new ResponseEntity<>(
                new ErrorDto(1003, "Requested resource is not a file."),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorDto> handleFileNotFoundException(FileNotFoundException e) {
        logger.warn(e.getMessage());
        return new ResponseEntity<>(
                new ErrorDto(1004, "Requested file could not be found."),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MaxDirectoryDownloadSizeExceededException.class)
    public ResponseEntity<ErrorDto> handleMaxDirectoryDownloadSizeExceededException(
            MaxDirectoryDownloadSizeExceededException e) {
        logger.warn(e.getMessage());
        return new ResponseEntity<>(
                new ErrorDto(1005, "This directory is bigger than maximum " +
                        "allowed size for zipped directory downloads."),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleUncaughtException(Exception e) {
        logger.error(e.getMessage(), e);
        return new ResponseEntity<>(
                new ErrorDto(2001, "Internal server error."),
                HttpStatus.NOT_FOUND);
    }
}