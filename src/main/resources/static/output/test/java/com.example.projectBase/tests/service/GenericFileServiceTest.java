package com.example.projectBase.tests.service;

import com.example.projectBase.tests.ApplicationTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.jdbc.JdbcTestUtils;

import javax.transaction.Transactional;

import com.example.projectBase.builder.GenericFileBuilder;
import com.example.projectBase.service.GenericFileService;
import com.example.projectBase.domain.GenericFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;


@Transactional
public class GenericFileServiceTest extends ApplicationTests {

    @Autowired
    GenericFileService genericFileService;

    @Test
    public void findAll_withContentInDB_returnsListWithContent() {
        GenericFile genericFile = GenericFileBuilder.generic().build();
        entityManager.persist(genericFile);
        entityManager.flush();


        List<GenericFile> genericFiles = genericFileService.findAll();

        assertNotNull(genericFiles);
        assertTrue(genericFiles.size() > 0);
    }

    @Test
    public void findById_withContentInDBAndValidId_returnsContent() {
        GenericFile genericFile = GenericFileBuilder.generic().build();
        entityManager.persist(genericFile);
        entityManager.flush();

        GenericFile genericFileInDB = genericFileService.findById(genericFile.getId());

        assertNotNull(genericFile);
        assertEquals(genericFile.getId(), genericFileInDB.getId());
    }

    @Test
    public void findById_withNullId_throwsException() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            genericFileService.findById(null);
        });
        assertEquals("The field id is obligatory", thrown.getMessage());
    }

    @Test
    public void findById_withNonExistentId_throwsException() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            genericFileService.findById(-1L);
        });
        assertEquals("Could not find entity genericFile with id: " + -1L, thrown.getMessage());
    }

    @Test
    public void save_withValidInfo_savesContent() {
        GenericFile genericFile = GenericFileBuilder.generic().build();
        GenericFile genericFileInDB = genericFileService.save(genericFile);
        assertNotNull(genericFileInDB);
        assertNotNull(genericFileInDB.getId());
    }

    @Test
    public void edit_withValidInfo_savesContent() {
        GenericFile genericFile = GenericFileBuilder.generic().build();
        entityManager.persist(genericFile);

        GenericFile genericFileInDB = genericFileService.edit(genericFile);

        assertNotNull(genericFileInDB);
        assertNotNull(genericFileInDB.getId());
    }


    @Test
    public void delete_withValidId_deletesValue() {
        GenericFile genericFile = GenericFileBuilder.generic().build();
        entityManager.persist(genericFile);
        entityManager.flush();
        int numberOfRowsInDBBefore = JdbcTestUtils.countRowsInTable(jdbcTemplate, "generic_file");

        genericFileService.delete(genericFile.getId());

        entityManager.flush();
        int numberOfRowsInDBAfter = JdbcTestUtils.countRowsInTable(jdbcTemplate, "generic_file");
        assertEquals(numberOfRowsInDBBefore, numberOfRowsInDBAfter + 1);
    }

    @Test
    public void delete_withNonExsistentId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            genericFileService.delete(-1L);
        });
    }

    @Test
    public void delete_withNullId_throwsException() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            genericFileService.delete(null);
        });
        assertEquals("The field id is obligatory", thrown.getMessage());
    }

}
