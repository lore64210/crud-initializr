package com.example.projectBase.controller.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Collections;
import java.util.Map;


@ControllerAdvice
public class AdviceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdviceController.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException exception) {
        LOGGER.warn("No encontrado: {}", exception.getMessage());
        return new ResponseEntity<>(Collections.singletonMap("error", exception.getMessage()),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException exception) {
        LOGGER.warn("Solicitud inválida: {}", exception.getMessage());
        return new ResponseEntity<>(Collections.singletonMap("error", exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleConflict(DataIntegrityViolationException exception) {
        LOGGER.warn("Conflicto de integridad: {}", exception.getMostSpecificCause().getMessage());
        return new ResponseEntity<>(Collections.singletonMap("error",
                "No se pudo guardar: viola una restricción de integridad (por ejemplo, una relación 1:1 duplicada)."),
                HttpStatus.CONFLICT);
    }
}
