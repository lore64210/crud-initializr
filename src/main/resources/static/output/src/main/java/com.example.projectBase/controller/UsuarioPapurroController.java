package com.example.projectBase.controller;

import com.example.projectBase.controller.advice.AdviceController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import com.example.projectBase.domain.UsuarioPapurro;
import com.example.projectBase.service.UsuarioPapurroService;

import java.util.List;

@RestController
@RequestMapping("/api/usuarioPapurro")
@RequiredArgsConstructor
public class UsuarioPapurroController extends AdviceController {

    UsuarioPapurroService usuarioPapurroService;


    @GetMapping
    public ResponseEntity<List<UsuarioPapurro>> findAll() {
        return ResponseEntity.ok(usuarioPapurroService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioPapurro> findById(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioPapurroService.findById(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioPapurro> save(@RequestBody UsuarioPapurro usuarioPapurro) {
        return ResponseEntity.ok(usuarioPapurroService.save(usuarioPapurro));
    }

    @PutMapping
    public ResponseEntity<UsuarioPapurro> edit(@RequestBody UsuarioPapurro usuarioPapurro) {
        return ResponseEntity.ok(usuarioPapurroService.edit(usuarioPapurro));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        usuarioPapurroService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
