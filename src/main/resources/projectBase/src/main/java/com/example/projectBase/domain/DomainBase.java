package com.example.projectBase.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
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
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    DOMAIN_ATTRIBUTES
}
