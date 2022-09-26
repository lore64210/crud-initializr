package com.example.projectBase.domain;

import lombok.Data;
import javax.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;



@Data
@Entity
@Table(name = "generic_file")
public class GenericFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @CreationTimestamp
    private LocalDateTime fechaCreacion;

    
	private String projectOwner;

	private String url;

}
