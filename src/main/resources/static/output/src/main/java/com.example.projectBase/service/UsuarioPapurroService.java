package com.example.projectBase.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import com.example.projectBase.repository.UsuarioPapurroRepository;
import com.example.projectBase.domain.UsuarioPapurro;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UsuarioPapurroService {

    
    private final UsuarioPapurroRepository usuarioPapurroRepository;

    public List<UsuarioPapurro> findAll() {
        return usuarioPapurroRepository.findAll();
    }

    public UsuarioPapurro findById(Long id) {
        Assert.notNull(id, "The field id is obligatory");
        try {
            return usuarioPapurroRepository.findById(id).orElseThrow();
        } catch (NoSuchElementException exception) {
            throw new IllegalArgumentException("Could not find entity usuarioPapurro with id: " + id);
        }
    }

    public UsuarioPapurro save(UsuarioPapurro usuarioPapurro) {
        validateInfo(usuarioPapurro);
        
        return usuarioPapurroRepository.save(usuarioPapurro);
    }

    public UsuarioPapurro edit(UsuarioPapurro usuarioPapurro) {
        Assert.notNull(usuarioPapurro.getId(), "The field id is obligatory");
        validateInfo(usuarioPapurro);
        
        return usuarioPapurroRepository.save(usuarioPapurro);
    }

    public void delete(Long id) {
        Assert.notNull(id, "The field id is obligatory");
        try {
            usuarioPapurroRepository.deleteById(id);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Could not find entity usuarioPapurro with id: " + id);
        }
    }

    private void validateInfo(UsuarioPapurro usuarioPapurro) {
        
    }

}
