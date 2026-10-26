package com.example.projectBase.domain;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

IMPORTS

@Data
@Document(collection = "TABLE_NAME")
public class DOMAIN_NAME {

    @Id
    private String id;

    @CreatedDate
    private LocalDateTime fechaCreacion;

    DOMAIN_ATTRIBUTES
}
