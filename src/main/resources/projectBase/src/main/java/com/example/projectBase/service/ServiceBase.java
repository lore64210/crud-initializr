package com.example.projectBase.service;

import com.example.projectBase.controller.advice.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import com.example.projectBase.repository.REPOSITORY_DECLARATION;
import com.example.projectBase.domain.DOMAIN_DECLARATION;
RELATION_IMPORTS
import java.util.List;
ARRAY_LIST_IMPORT
import java.util.NoSuchElementException;

CACHE_IMPORTS
@Service
@RequiredArgsConstructor
public class SERVICE_DECLARATION {

    RELATION_REPOSITORIES
    private final REPOSITORY_DECLARATION REPOSITORY;

    CACHE_FIND_ALL
    public List<DOMAIN_DECLARATION> findAll() {
        return REPOSITORY.findAll();
    }

    CACHE_FIND_BY_ID
    public DOMAIN_DECLARATION findById(Long id) {
        Assert.notNull(id, "The field id is obligatory");
        try {
            return REPOSITORY.findById(id).orElseThrow();
        } catch (NoSuchElementException exception) {
            throw new NotFoundException("Could not find entity DOMAIN with id: " + id);
        }
    }

    @Transactional
    CACHE_SAVE
    public DOMAIN_DECLARATION save(DOMAIN_DECLARATION DOMAIN) {
        attachRelations(DOMAIN);
        validateInfo(DOMAIN);
        return REPOSITORY.save(DOMAIN);
    }

    @Transactional
    CACHE_EDIT
    public DOMAIN_DECLARATION edit(DOMAIN_DECLARATION DOMAIN) {
        Assert.notNull(DOMAIN.getId(), "The field id is obligatory");
        DOMAIN_DECLARATION existing = REPOSITORY.findById(DOMAIN.getId())
                .orElseThrow(() -> new NotFoundException("Could not find entity DOMAIN with id: " + DOMAIN.getId()));
        DOMAIN.setFechaCreacion(existing.getFechaCreacion());
        attachRelations(DOMAIN);
        validateInfo(DOMAIN);
        return REPOSITORY.save(DOMAIN);
    }

    CACHE_DELETE
    public void delete(Long id) {
        Assert.notNull(id, "The field id is obligatory");
        if (!REPOSITORY.existsById(id)) {
            throw new NotFoundException("Could not find entity DOMAIN with id: " + id);
        }
        REPOSITORY.deleteById(id);
    }

    private void attachRelations(DOMAIN_DECLARATION DOMAIN) {
        ATTACH_RELATIONS
    }

    private void validateInfo(DOMAIN_DECLARATION DOMAIN) {
        VALIDATIONS
    }

}
