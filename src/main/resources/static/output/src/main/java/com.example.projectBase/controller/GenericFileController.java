package com.example.projectBase.controller;

import com.example.projectBase.controller.advice.AdviceController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import com.example.projectBase.domain.GenericFile;
import com.example.projectBase.service.GenericFileService;

import java.util.List;

@RestController
@RequestMapping("/api/genericFile")
@RequiredArgsConstructor
public class GenericFileController extends AdviceController {

    GenericFileService genericFileService;


    @GetMapping
    public ResponseEntity<List<GenericFile>> findAll() {
        return ResponseEntity.ok(genericFileService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GenericFile> findById(@PathVariable Long id) {
        return ResponseEntity.ok(genericFileService.findById(id));
    }

    @PostMapping
    public ResponseEntity<GenericFile> save(@RequestBody GenericFile genericFile) {
        return ResponseEntity.ok(genericFileService.save(genericFile));
    }

    @PutMapping
    public ResponseEntity<GenericFile> edit(@RequestBody GenericFile genericFile) {
        return ResponseEntity.ok(genericFileService.edit(genericFile));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        genericFileService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
