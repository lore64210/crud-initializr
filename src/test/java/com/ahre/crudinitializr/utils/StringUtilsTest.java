package com.ahre.crudinitializr.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringUtilsTest {

    @Test
    void capitalizeFirstCharacter_withLowerCaseValue_returnsCapitalized() {
        assertEquals("Persona", StringUtils.capitalizeFirstCharacter("persona"));
    }

    @Test
    void capitalizeFirstCharacter_withEmptyValue_returnsEmpty() {
        assertEquals("", StringUtils.capitalizeFirstCharacter(""));
    }

    @Test
    void lowerFirstCharacter_withUpperCaseValue_returnsLowerCase() {
        assertEquals("personaService", StringUtils.lowerFirstCharacter("PersonaService"));
    }

    @Test
    void transformToSnakeCase_withCamelCase_returnsSnakeCase() {
        assertEquals("fecha_creacion", StringUtils.transformToSnakeCase("fechaCreacion"));
    }

    @Test
    void transformToSnakeCase_withSpaces_returnsSnakeCase() {
        assertEquals("mi_entidad", StringUtils.transformToSnakeCase("Mi Entidad"));
    }

    @Test
    void toArtifactId_withTitle_returnsKebabCase() {
        assertEquals("mi-proyecto", StringUtils.toArtifactId("Mi Proyecto"));
    }

    @Test
    void toArtifactId_withBlankValue_returnsDefault() {
        assertEquals("project-base", StringUtils.toArtifactId("   "));
    }

    @Test
    void toArtifactId_withRepeatedSeparators_returnsSingleDashes() {
        assertEquals("mi-proyecto", StringUtils.toArtifactId("Mi  &  Proyecto"));
    }

    @Test
    void toClassName_withTitle_returnsPascalCase() {
        assertEquals("MiProyecto", StringUtils.toClassName("Mi Proyecto"));
    }

    @Test
    void toClassName_withLeadingDigit_prefixesProject() {
        assertEquals("Project123Tienda", StringUtils.toClassName("123 Tienda"));
    }

    @Test
    void toDatabaseName_withTitle_returnsSnakeCase() {
        assertEquals("mi_proyecto", StringUtils.toDatabaseName("Mi Proyecto"));
    }

    @Test
    void toDatabaseName_withLeadingDigit_prefixesDb() {
        assertEquals("db_123_tienda", StringUtils.toDatabaseName("123 Tienda"));
    }

    @Test
    void escapeXml_withSpecialCharacters_escapesThem() {
        assertEquals("a &amp; b &lt;c&gt; &quot;d&quot; &apos;e&apos;",
                StringUtils.escapeXml("a & b <c> \"d\" 'e'"));
    }
}
