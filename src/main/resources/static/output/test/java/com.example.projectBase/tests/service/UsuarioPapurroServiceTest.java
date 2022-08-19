package com.example.projectBase.tests.service;

import com.example.projectBase.tests.ApplicationTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.jdbc.JdbcTestUtils;

import javax.transaction.Transactional;

import com.example.projectBase.builder.UsuarioPapurroBuilder;
import com.example.projectBase.service.UsuarioPapurroService;
import com.example.projectBase.domain.UsuarioPapurro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;


@Transactional
public class UsuarioPapurroServiceTest extends ApplicationTests {

    @Autowired
    UsuarioPapurroService usuarioPapurroService;

    @Test
    public void findAll_withContentInDB_returnsListWithContent() {
        UsuarioPapurro usuarioPapurro = UsuarioPapurroBuilder.generic().build();
        entityManager.persist(usuarioPapurro);
        entityManager.flush();


        List<UsuarioPapurro> usuarioPapurros = usuarioPapurroService.findAll();

        assertNotNull(usuarioPapurros);
        assertTrue(usuarioPapurros.size() > 0);
    }

    @Test
    public void findById_withContentInDBAndValidId_returnsContent() {
        UsuarioPapurro usuarioPapurro = UsuarioPapurroBuilder.generic().build();
        entityManager.persist(usuarioPapurro);
        entityManager.flush();

        UsuarioPapurro usuarioPapurroInDB = usuarioPapurroService.findById(usuarioPapurro.getId());

        assertNotNull(usuarioPapurro);
        assertEquals(usuarioPapurro.getId(), usuarioPapurroInDB.getId());
    }

    @Test
    public void findById_withNullId_throwsException() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            usuarioPapurroService.findById(null);
        });
        assertEquals("The field id is obligatory", thrown.getMessage());
    }

    @Test
    public void findById_withNonExistentId_throwsException() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            usuarioPapurroService.findById(-1L);
        });
        assertEquals("Could not find entity usuarioPapurro with id: " + -1L, thrown.getMessage());
    }

    @Test
    public void save_withValidInfo_savesContent() {
        UsuarioPapurro usuarioPapurro = UsuarioPapurroBuilder.generic().build();
        UsuarioPapurro usuarioPapurroInDB = usuarioPapurroService.save(usuarioPapurro);
        assertNotNull(usuarioPapurroInDB);
        assertNotNull(usuarioPapurroInDB.getId());
    }

    @Test
    public void edit_withValidInfo_savesContent() {
        UsuarioPapurro usuarioPapurro = UsuarioPapurroBuilder.generic().build();
        entityManager.persist(usuarioPapurro);

        UsuarioPapurro usuarioPapurroInDB = usuarioPapurroService.edit(usuarioPapurro);

        assertNotNull(usuarioPapurroInDB);
        assertNotNull(usuarioPapurroInDB.getId());
    }


    @Test
    public void delete_withValidId_deletesValue() {
        UsuarioPapurro usuarioPapurro = UsuarioPapurroBuilder.generic().build();
        entityManager.persist(usuarioPapurro);
        entityManager.flush();
        int numberOfRowsInDBBefore = JdbcTestUtils.countRowsInTable(jdbcTemplate, "usuario_papurro");

        usuarioPapurroService.delete(usuarioPapurro.getId());

        entityManager.flush();
        int numberOfRowsInDBAfter = JdbcTestUtils.countRowsInTable(jdbcTemplate, "usuario_papurro");
        assertEquals(numberOfRowsInDBBefore, numberOfRowsInDBAfter + 1);
    }

    @Test
    public void delete_withNonExsistentId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            usuarioPapurroService.delete(-1L);
        });
    }

    @Test
    public void delete_withNullId_throwsException() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            usuarioPapurroService.delete(null);
        });
        assertEquals("The field id is obligatory", thrown.getMessage());
    }

}
