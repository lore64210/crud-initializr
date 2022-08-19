package com.example.projectBase.domain;

import lombok.Data;
import javax.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;



@Data
@Entity
@Table(name = "entrevista_principal")
public class EntrevistaPrincipal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @CreationTimestamp
    private LocalDateTime fechaCreacion;

    
	private String tituloChevere;

	@ManyToOne(cascade = CascadeType.ALL)
	private UsuarioPapurro usuarioPapurro;

}
