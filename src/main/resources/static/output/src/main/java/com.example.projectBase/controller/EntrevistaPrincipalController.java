package com.example.projectBase.controller;

import com.example.projectBase.controller.advice.AdviceController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import com.example.projectBase.domain.EntrevistaPrincipal;
import com.example.projectBase.service.EntrevistaPrincipalService;

import java.util.List;

@RestController
@RequestMapping("/api/entrevistaPrincipal")
@RequiredArgsConstructor
public class EntrevistaPrincipalController extends AdviceController {

    EntrevistaPrincipalService entrevistaPrincipalService;


    @GetMapping
    public ResponseEntity<List<EntrevistaPrincipal>> findAll() {
        return ResponseEntity.ok(entrevistaPrincipalService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntrevistaPrincipal> findById(@PathVariable Long id) {
        return ResponseEntity.ok(entrevistaPrincipalService.findById(id));
    }

    @PostMapping
    public ResponseEntity<EntrevistaPrincipal> save(@RequestBody EntrevistaPrincipal entrevistaPrincipal) {
        return ResponseEntity.ok(entrevistaPrincipalService.save(entrevistaPrincipal));
    }

    @PutMapping
    public ResponseEntity<EntrevistaPrincipal> edit(@RequestBody EntrevistaPrincipal entrevistaPrincipal) {
        return ResponseEntity.ok(entrevistaPrincipalService.edit(entrevistaPrincipal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        entrevistaPrincipalService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
