package com.example.projectBase.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.projectBase.domain.GenericFile;

public interface GenericFileRepository extends JpaRepository<GenericFile, Long> {
}
