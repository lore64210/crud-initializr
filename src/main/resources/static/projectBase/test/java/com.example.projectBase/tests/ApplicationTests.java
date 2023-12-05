package com.example.projectBase.tests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;

@SpringBootTest
public class ApplicationTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    public JdbcTemplate jdbcTemplate;

    @Autowired
    public EntityManager entityManager;


}
