package com.example.projectBase.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import com.example.projectBase.repository.GenericFileRepository;
import com.example.projectBase.domain.GenericFile;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class GenericFileService {

    
    private final GenericFileRepository genericFileRepository;

    public List<GenericFile> findAll() {
        return genericFileRepository.findAll();
    }

    public GenericFile findById(Long id) {
        Assert.notNull(id, "The field id is obligatory");
        try {
            return genericFileRepository.findById(id).orElseThrow();
        } catch (NoSuchElementException exception) {
            throw new IllegalArgumentException("Could not find entity genericFile with id: " + id);
        }
    }

    public GenericFile save(GenericFile genericFile) {
        validateInfo(genericFile);
        
        return genericFileRepository.save(genericFile);
    }

    public GenericFile edit(GenericFile genericFile) {
        Assert.notNull(genericFile.getId(), "The field id is obligatory");
        validateInfo(genericFile);
        
        return genericFileRepository.save(genericFile);
    }

    public void delete(Long id) {
        Assert.notNull(id, "The field id is obligatory");
        try {
            genericFileRepository.deleteById(id);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Could not find entity genericFile with id: " + id);
        }
    }

    private void validateInfo(GenericFile genericFile) {
        
	Assert.notNull(genericFile.getProjectOwner(), "projectOwner can not be null");

    }

}
