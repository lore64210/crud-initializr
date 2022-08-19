package com.example.projectBase.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import com.example.projectBase.repository.REPOSITORY_DECLARATION;
import com.example.projectBase.domain.DOMAIN_DECLARATION;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class SERVICE_DECLARATION {

    EXTERNAL_ENTITIES_SERVICES
    private final REPOSITORY_DECLARATION REPOSITORY;

    public List<DOMAIN_DECLARATION> findAll() {
        return REPOSITORY.findAll();
    }

    public DOMAIN_DECLARATION findById(Long id) {
        Assert.notNull(id, "The field id is obligatory");
        try {
            return REPOSITORY.findById(id).orElseThrow();
        } catch (NoSuchElementException exception) {
            throw new IllegalArgumentException("Could not find entity DOMAIN with id: " + id);
        }
    }

    public DOMAIN_DECLARATION save(DOMAIN_DECLARATION DOMAIN) {
        validateInfo(DOMAIN);
        SAVE_EXTERNAL_ENTITIES
        return REPOSITORY.save(DOMAIN);
    }

    public DOMAIN_DECLARATION edit(DOMAIN_DECLARATION DOMAIN) {
        Assert.notNull(DOMAIN.getId(), "The field id is obligatory");
        validateInfo(DOMAIN);
        SAVE_EXTERNAL_ENTITIES
        return REPOSITORY.save(DOMAIN);
    }

    public void delete(Long id) {
        Assert.notNull(id, "The field id is obligatory");
        try {
            REPOSITORY.deleteById(id);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Could not find entity DOMAIN with id: " + id);
        }
    }

    private void validateInfo(DOMAIN_DECLARATION DOMAIN) {
        VALIDATIONS
    }

}
