package com.example.projectBase.controller;

import com.example.projectBase.controller.advice.AdviceController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
IMPORTS
CACHE_IMPORTS

import java.util.List;

@Tag(name = "ENTITY_DECLARATION", description = "Operaciones CRUD de ENTITY_DECLARATION")
@RestController
@RequestMapping("REQUEST_MAPPING")
@RequiredArgsConstructor
public class ENTITY_DECLARATIONController extends AdviceController {

    private final SERVICE_DECLARATION

    CACHE_FIND_ALL
    @Operation(summary = "Listar todas las entidades ENTITY_DECLARATION")
    @GetMapping
    public List<RESPONSE_TYPE> findAll() {
        FIND_ALL_BODY
    }

    CACHE_FIND_BY_ID
    @Operation(summary = "Buscar ENTITY_DECLARATION por id")
    @GetMapping("/{id}")
    public RESPONSE_TYPE findById(
            @PathVariable @Parameter(description = "ID de ENTITY_DECLARATION") Long id) {
        FIND_BY_ID_BODY
    }

    CACHE_SAVE
    @Operation(summary = "Crear ENTITY_DECLARATION")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RESPONSE_TYPE save(@RequestBody REQUEST_TYPE request) {
        SAVE_BODY
    }

    CACHE_EDIT
    @Operation(summary = "Actualizar ENTITY_DECLARATION")
    @PutMapping
    public RESPONSE_TYPE edit(@RequestBody REQUEST_TYPE request) {
        EDIT_BODY
    }

    CACHE_DELETE
    @Operation(summary = "Eliminar ENTITY_DECLARATION")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable @Parameter(description = "ID de ENTITY_DECLARATION") Long id) {
        SERVICE.delete(id);
        return ResponseEntity.noContent().build();
    }

}
