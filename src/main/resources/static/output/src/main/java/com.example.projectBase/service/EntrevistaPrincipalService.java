package com.example.projectBase.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import com.example.projectBase.repository.EntrevistaPrincipalRepository;
import com.example.projectBase.domain.EntrevistaPrincipal;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class EntrevistaPrincipalService {

    
	private final UsuarioPapurroService usuarioPapurroService;

    private final EntrevistaPrincipalRepository entrevistaPrincipalRepository;

    public List<EntrevistaPrincipal> findAll() {
        return entrevistaPrincipalRepository.findAll();
    }

    public EntrevistaPrincipal findById(Long id) {
        Assert.notNull(id, "The field id is obligatory");
        try {
            return entrevistaPrincipalRepository.findById(id).orElseThrow();
        } catch (NoSuchElementException exception) {
            throw new IllegalArgumentException("Could not find entity entrevistaPrincipal with id: " + id);
        }
    }

    public EntrevistaPrincipal save(EntrevistaPrincipal entrevistaPrincipal) {
        validateInfo(entrevistaPrincipal);
        
		usuarioPapurroService.save(entrevistaPrincipal.getUsuarioPapurro());

        return entrevistaPrincipalRepository.save(entrevistaPrincipal);
    }

    public EntrevistaPrincipal edit(EntrevistaPrincipal entrevistaPrincipal) {
        Assert.notNull(entrevistaPrincipal.getId(), "The field id is obligatory");
        validateInfo(entrevistaPrincipal);
        
		usuarioPapurroService.save(entrevistaPrincipal.getUsuarioPapurro());

        return entrevistaPrincipalRepository.save(entrevistaPrincipal);
    }

    public void delete(Long id) {
        Assert.notNull(id, "The field id is obligatory");
        try {
            entrevistaPrincipalRepository.deleteById(id);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Could not find entity entrevistaPrincipal with id: " + id);
        }
    }

    private void validateInfo(EntrevistaPrincipal entrevistaPrincipal) {
        
	Assert.isTrue(!entrevistaPrincipal.getTituloChevere().isEmpty(), "tituloChevere can not be blank");

	Assert.notNull(entrevistaPrincipal.getTituloChevere(), "tituloChevere can not be null");

	Assert.notNull(entrevistaPrincipal.getUsuarioPapurro(), "usuarioPapurro can not be null");

    }

}
