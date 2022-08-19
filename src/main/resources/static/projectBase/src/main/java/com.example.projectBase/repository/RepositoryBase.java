package com.example.projectBase.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.projectBase.domain.ENTITY;

public interface ENTITYRepository extends JpaRepository<ENTITY, Long> {
}
