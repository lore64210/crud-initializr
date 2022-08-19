package com.ahre.crudinitializr.controller.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Collections;
import java.util.Map;


@ControllerAdvice
public class AdviceController {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException exception) {
        exception.printStackTrace();
        return new ResponseEntity<>(Collections.singletonMap("error", exception.getMessage()), HttpStatus.BAD_REQUEST);
    }
}
