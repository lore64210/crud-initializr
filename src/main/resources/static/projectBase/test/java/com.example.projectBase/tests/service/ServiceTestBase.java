package com.example.projectBase.tests.service;

import com.example.projectBase.tests.ApplicationTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.jdbc.JdbcTestUtils;
import jakarta.transaction.Transactional;
import com.example.projectBase.builder.DOMAIN_DECLARATIONBuilder;
import com.example.projectBase.service.SERVICE_DECLARATION;
import com.example.projectBase.domain.DOMAIN_DECLARATION;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;


@Transactional
public class DOMAIN_DECLARATIONServiceTest extends ApplicationTests {

    @Autowired
    SERVICE_DECLARATION SERVICE;

    @Test
    public void findAll_withContentInDB_returnsListWithContent() {
        DOMAIN_DECLARATION DOMAIN = DOMAIN_DECLARATIONBuilder.generic().build();
        entityManager.persist(DOMAIN);
        entityManager.flush();


        List<DOMAIN_DECLARATION> DOMAINs = SERVICE.findAll();

        assertNotNull(DOMAINs);
        assertTrue(DOMAINs.size() > 0);
    }

    @Test
    public void findById_withContentInDBAndValidId_returnsContent() {
        DOMAIN_DECLARATION DOMAIN = DOMAIN_DECLARATIONBuilder.generic().build();
        entityManager.persist(DOMAIN);
        entityManager.flush();

        DOMAIN_DECLARATION DOMAINInDB = SERVICE.findById(DOMAIN.getId());

        assertNotNull(DOMAIN);
        assertEquals(DOMAIN.getId(), DOMAINInDB.getId());
    }

    @Test
    public void findById_withNullId_throwsException() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            SERVICE.findById(null);
        });
        assertEquals("The field id is obligatory", thrown.getMessage());
    }

    @Test
    public void findById_withNonExistentId_throwsException() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            SERVICE.findById(-1L);
        });
        assertEquals("Could not find entity DOMAIN with id: " + -1L, thrown.getMessage());
    }

    @Test
    public void save_withValidInfo_savesContent() {
        DOMAIN_DECLARATION DOMAIN = DOMAIN_DECLARATIONBuilder.generic().build();
        DOMAIN_DECLARATION DOMAINInDB = SERVICE.save(DOMAIN);
        assertNotNull(DOMAINInDB);
        assertNotNull(DOMAINInDB.getId());
    }

    @Test
    public void edit_withValidInfo_savesContent() {
        DOMAIN_DECLARATION DOMAIN = DOMAIN_DECLARATIONBuilder.generic().build();
        entityManager.persist(DOMAIN);

        DOMAIN_DECLARATION DOMAINInDB = SERVICE.edit(DOMAIN);

        assertNotNull(DOMAINInDB);
        assertNotNull(DOMAINInDB.getId());
    }


    @Test
    public void delete_withValidId_deletesValue() {
        DOMAIN_DECLARATION DOMAIN = DOMAIN_DECLARATIONBuilder.generic().build();
        entityManager.persist(DOMAIN);
        entityManager.flush();
        int numberOfRowsInDBBefore = JdbcTestUtils.countRowsInTable(jdbcTemplate, "DOMAIN_DATABASE");

        SERVICE.delete(DOMAIN.getId());

        entityManager.flush();
        int numberOfRowsInDBAfter = JdbcTestUtils.countRowsInTable(jdbcTemplate, "DOMAIN_DATABASE");
        assertEquals(numberOfRowsInDBBefore, numberOfRowsInDBAfter + 1);
    }

    @Test
    public void delete_withNonExsistentId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            SERVICE.delete(-1L);
        });
    }

    @Test
    public void delete_withNullId_throwsException() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            SERVICE.delete(null);
        });
        assertEquals("The field id is obligatory", thrown.getMessage());
    }

}
