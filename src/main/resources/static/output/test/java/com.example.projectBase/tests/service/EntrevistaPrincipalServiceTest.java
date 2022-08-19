package com.example.projectBase.tests.service;

import com.example.projectBase.tests.ApplicationTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.jdbc.JdbcTestUtils;

import javax.transaction.Transactional;

import com.example.projectBase.builder.EntrevistaPrincipalBuilder;
import com.example.projectBase.service.EntrevistaPrincipalService;
import com.example.projectBase.domain.EntrevistaPrincipal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;


@Transactional
public class EntrevistaPrincipalServiceTest extends ApplicationTests {

    @Autowired
    EntrevistaPrincipalService entrevistaPrincipalService;

    @Test
    public void findAll_withContentInDB_returnsListWithContent() {
        EntrevistaPrincipal entrevistaPrincipal = EntrevistaPrincipalBuilder.generic().build();
        entityManager.persist(entrevistaPrincipal);
        entityManager.flush();


        List<EntrevistaPrincipal> entrevistaPrincipals = entrevistaPrincipalService.findAll();

        assertNotNull(entrevistaPrincipals);
        assertTrue(entrevistaPrincipals.size() > 0);
    }

    @Test
    public void findById_withContentInDBAndValidId_returnsContent() {
        EntrevistaPrincipal entrevistaPrincipal = EntrevistaPrincipalBuilder.generic().build();
        entityManager.persist(entrevistaPrincipal);
        entityManager.flush();

        EntrevistaPrincipal entrevistaPrincipalInDB = entrevistaPrincipalService.findById(entrevistaPrincipal.getId());

        assertNotNull(entrevistaPrincipal);
        assertEquals(entrevistaPrincipal.getId(), entrevistaPrincipalInDB.getId());
    }

    @Test
    public void findById_withNullId_throwsException() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            entrevistaPrincipalService.findById(null);
        });
        assertEquals("The field id is obligatory", thrown.getMessage());
    }

    @Test
    public void findById_withNonExistentId_throwsException() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            entrevistaPrincipalService.findById(-1L);
        });
        assertEquals("Could not find entity entrevistaPrincipal with id: " + -1L, thrown.getMessage());
    }

    @Test
    public void save_withValidInfo_savesContent() {
        EntrevistaPrincipal entrevistaPrincipal = EntrevistaPrincipalBuilder.generic().build();
        EntrevistaPrincipal entrevistaPrincipalInDB = entrevistaPrincipalService.save(entrevistaPrincipal);
        assertNotNull(entrevistaPrincipalInDB);
        assertNotNull(entrevistaPrincipalInDB.getId());
    }

    @Test
    public void edit_withValidInfo_savesContent() {
        EntrevistaPrincipal entrevistaPrincipal = EntrevistaPrincipalBuilder.generic().build();
        entityManager.persist(entrevistaPrincipal);

        EntrevistaPrincipal entrevistaPrincipalInDB = entrevistaPrincipalService.edit(entrevistaPrincipal);

        assertNotNull(entrevistaPrincipalInDB);
        assertNotNull(entrevistaPrincipalInDB.getId());
    }


    @Test
    public void delete_withValidId_deletesValue() {
        EntrevistaPrincipal entrevistaPrincipal = EntrevistaPrincipalBuilder.generic().build();
        entityManager.persist(entrevistaPrincipal);
        entityManager.flush();
        int numberOfRowsInDBBefore = JdbcTestUtils.countRowsInTable(jdbcTemplate, "entrevista_principal");

        entrevistaPrincipalService.delete(entrevistaPrincipal.getId());

        entityManager.flush();
        int numberOfRowsInDBAfter = JdbcTestUtils.countRowsInTable(jdbcTemplate, "entrevista_principal");
        assertEquals(numberOfRowsInDBBefore, numberOfRowsInDBAfter + 1);
    }

    @Test
    public void delete_withNonExsistentId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            entrevistaPrincipalService.delete(-1L);
        });
    }

    @Test
    public void delete_withNullId_throwsException() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            entrevistaPrincipalService.delete(null);
        });
        assertEquals("The field id is obligatory", thrown.getMessage());
    }

}
