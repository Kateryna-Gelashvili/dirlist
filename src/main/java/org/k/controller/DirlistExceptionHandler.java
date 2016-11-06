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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


@ControllerAdvice
public class DirlistExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(DirlistExceptionHandler.class);

    @ExceptionHandler(RangeNotSatisfiableException.class)
    public ResponseEntity<String> handleRangeNotSatisfiableException(
            RangeNotSatisfiableException e) {
        logger.warn(e.getMessage());
        return new ResponseEntity<>(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
    }

    @ExceptionHandler(NotDirectoryException.class)
    public ResponseEntity<ErrorDto> handleNotDirectoryException(NotDirectoryException e) {
        logger.warn(e.getMessage());
        return responseWithError(HttpStatus.BAD_REQUEST,
                1001, "Requested resource is not a directory.");
    }


    @ExceptionHandler(DirectoryNotFoundException.class)
    public ResponseEntity<ErrorDto> handleDirectoryNotFoundException(DirectoryNotFoundException e) {
        logger.warn(e.getMessage());
        return responseWithError(HttpStatus.NOT_FOUND,
                1002, "Requested directory could not be found.");
    }

    @ExceptionHandler(NotFileException.class)
    public ResponseEntity<ErrorDto> handleNotFileException(NotFileException e) {
        logger.warn(e.getMessage());
        return responseWithError(HttpStatus.BAD_REQUEST,
                1003, "Requested resource is not a file."
        );
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorDto> handleFileNotFoundException(FileNotFoundException e) {
        logger.warn(e.getMessage());
        return responseWithError(HttpStatus.NOT_FOUND,
                1004, "Requested file could not be found."
        );
    }

    @ExceptionHandler(MaxDirectoryDownloadSizeExceededException.class)
    public ResponseEntity<ErrorDto> handleMaxDirectoryDownloadSizeExceededException(
            MaxDirectoryDownloadSizeExceededException e) {
        logger.warn(e.getMessage());
        return responseWithError(HttpStatus.FORBIDDEN,
                1005, "This directory is bigger than maximum " +
                        "allowed size for zipped directory downloads."
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleUncaughtException(Exception e) {
        logger.error(e.getMessage(), e);
        return responseWithError(HttpStatus.NOT_FOUND,
                2001, "Internal server error."
        );
    }

    private ResponseEntity<ErrorDto> responseWithError(HttpStatus status,
                                                       int code, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        return new ResponseEntity<>(new ErrorDto(code, message), headers, status);
    }
}
