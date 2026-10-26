package com.example.projectBase.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.example.projectBase.domain.ENTITY;

public interface ENTITYRepository extends MongoRepository<ENTITY, String> {
}
