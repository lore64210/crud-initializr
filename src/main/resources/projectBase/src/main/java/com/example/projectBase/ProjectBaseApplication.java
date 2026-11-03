package com.example.projectBase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class ProjectBaseApplication {

    public static void main(String[] args) {
        // Evita alias de zona horaria que algunas imágenes de PostgreSQL ya no incluyen.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(ProjectBaseApplication.class, args);
    }

}
