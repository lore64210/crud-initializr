package com.example.projectBase.domain;

import lombok.Data;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

IMPORTS

@Data
@Entity
@Table(name = "TABLE_NAME")
public class DOMAIN_NAME {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @CreationTimestamp
    private LocalDateTime fechaCreacion;

    DOMAIN_ATTRIBUTES
}
