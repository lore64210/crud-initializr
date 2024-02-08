package com.example.projectBase.controller;

import com.example.projectBase.controller.advice.AdviceController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
IMPORTS
import java.util.List;

@RestController
@RequestMapping("REQUEST_MAPPING")
@RequiredArgsConstructor
public class ENTITY_DECLARATIONController extends AdviceController {

    private final SERVICE_DECLARATION

    @GetMapping
    public ResponseEntity<List<ENTITY_DECLARATION>> findAll() {
        return ResponseEntity.ok(SERVICE.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ENTITY_DECLARATION> findById(@PathVariable Long id) {
        return ResponseEntity.ok(SERVICE.findById(id));
    }

    @PostMapping
    public ResponseEntity<ENTITY_DECLARATION> save(@RequestBody ENTITY_DECLARATION ENTITY) {
        return ResponseEntity.ok(SERVICE.save(ENTITY));
    }

    @PutMapping
    public ResponseEntity<ENTITY_DECLARATION> edit(@RequestBody ENTITY_DECLARATION ENTITY) {
        return ResponseEntity.ok(SERVICE.edit(ENTITY));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        SERVICE.delete(id);
        return ResponseEntity.noContent().build();
    }

}
