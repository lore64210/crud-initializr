package com.example.projectBase.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import com.example.projectBase.repository.REPOSITORY_DECLARATION;
import com.example.projectBase.domain.DOMAIN_DECLARATION;

import java.util.List;
import java.util.NoSuchElementException;

CACHE_IMPORTS
@Service
@RequiredArgsConstructor
public class SERVICE_DECLARATION {

    private final REPOSITORY_DECLARATION REPOSITORY;

    CACHE_FIND_ALL
    public List<DOMAIN_DECLARATION> findAll() {
        return REPOSITORY.findAll();
    }

    CACHE_FIND_BY_ID
    public DOMAIN_DECLARATION findById(String id) {
        Assert.notNull(id, "The field id is obligatory");
        try {
            return REPOSITORY.findById(id).orElseThrow();
        } catch (NoSuchElementException exception) {
            throw new IllegalArgumentException("Could not find entity DOMAIN with id: " + id);
        }
    }

    CACHE_SAVE
    public DOMAIN_DECLARATION save(DOMAIN_DECLARATION DOMAIN) {
        validateInfo(DOMAIN);
        return REPOSITORY.save(DOMAIN);
    }

    CACHE_EDIT
    public DOMAIN_DECLARATION edit(DOMAIN_DECLARATION DOMAIN) {
        Assert.notNull(DOMAIN.getId(), "The field id is obligatory");
        validateInfo(DOMAIN);
        return REPOSITORY.save(DOMAIN);
    }

    CACHE_DELETE
    public void delete(String id) {
        Assert.notNull(id, "The field id is obligatory");
        if (!REPOSITORY.existsById(id)) {
            throw new IllegalArgumentException("Could not find entity DOMAIN with id: " + id);
        }
        REPOSITORY.deleteById(id);
    }

    private void validateInfo(DOMAIN_DECLARATION DOMAIN) {
        VALIDATIONS
    }

}
