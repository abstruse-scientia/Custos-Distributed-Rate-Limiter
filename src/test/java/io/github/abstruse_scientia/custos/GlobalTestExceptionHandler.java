package io.github.abstruse_scientia.custos;


import io.github.abstruse_scientia.custos.exception.RateLimitExceededException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalTestExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<String> handleRateLimitException(RateLimitExceededException ex) {
        return ResponseEntity.status(429).body(ex.getMessage());
    }

}
