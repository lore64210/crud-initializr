package com.example.projectBase.domain;

import lombok.Data;
import javax.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;



@Data
@Entity
@Table(name = "usuario_papurro")
public class UsuarioPapurro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @CreationTimestamp
    private LocalDateTime fechaCreacion;

    
	private Long niIdea;

}
