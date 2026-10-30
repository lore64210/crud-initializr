package com.example.projectBase.controller;

import com.example.projectBase.controller.advice.AdviceController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
IMPORTS
import java.util.List;

@Tag(name = "ENTITY_DECLARATION", description = "Operaciones CRUD de ENTITY_DECLARATION")
@RestController
@RequestMapping("REQUEST_MAPPING")
@RequiredArgsConstructor
public class ENTITY_DECLARATIONController extends AdviceController {

    private final SERVICE_DECLARATION

    @Operation(summary = "Listar todas las entidades ENTITY_DECLARATION")
    @GetMapping
    public ResponseEntity<List<ENTITY_DECLARATION>> findAll() {
        return ResponseEntity.ok(SERVICE.findAll());
    }

    @Operation(summary = "Buscar ENTITY_DECLARATION por id")
    @GetMapping("/{id}")
    public ResponseEntity<ENTITY_DECLARATION> findById(
            @PathVariable @Parameter(description = "ID de ENTITY_DECLARATION") Long id) {
        return ResponseEntity.ok(SERVICE.findById(id));
    }

    @Operation(summary = "Crear ENTITY_DECLARATION")
    @PostMapping
    public ResponseEntity<ENTITY_DECLARATION> save(@RequestBody ENTITY_DECLARATION ENTITY) {
        return ResponseEntity.ok(SERVICE.save(ENTITY));
    }

    @Operation(summary = "Actualizar ENTITY_DECLARATION")
    @PutMapping
    public ResponseEntity<ENTITY_DECLARATION> edit(@RequestBody ENTITY_DECLARATION ENTITY) {
        return ResponseEntity.ok(SERVICE.edit(ENTITY));
    }

    @Operation(summary = "Eliminar ENTITY_DECLARATION")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable @Parameter(description = "ID de ENTITY_DECLARATION") Long id) {
        SERVICE.delete(id);
        return ResponseEntity.noContent().build();
    }

}
