package com.broadcastMail.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ProblemDetail> handleAlreadyuserExistException(UserAlreadyExistsException ex)
    {
        ProblemDetail problemDetail=ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setProperty("errorMessage", ex.getMessage());
        problemDetail.setStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setType(URI.create("/error"));
        return ResponseEntity.ok().body(problemDetail);

    }


    @ExceptionHandler(InvalidFileExtensionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ProblemDetail> handleInvalidException(InvalidFileExtensionException ex)
    {
        ProblemDetail problemDetail=ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setProperty("errorMessage", ex.getMessage());
        problemDetail.setStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setType(URI.create("/error"));
        return ResponseEntity.ok().body(problemDetail);

    }

    @ExceptionHandler(FolderDoesNotExistsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ProblemDetail> handleFolderNotExistsException(FolderDoesNotExistsException ex)
    {
        ProblemDetail problemDetail=ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setProperty("errorMessage", ex.getMessage());
        problemDetail.setStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setType(URI.create("/error"));
        return ResponseEntity.ok().body(problemDetail);

    }
}
