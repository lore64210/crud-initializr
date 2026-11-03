package com.example.projectBase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.util.TimeZone;

@SpringBootApplication
@EnableMongoAuditing
public class ProjectBaseApplication {

    public static void main(String[] args) {
        // Zona horaria fija para que el comportamiento sea consistente entre entornos.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(ProjectBaseApplication.class, args);
    }

}
