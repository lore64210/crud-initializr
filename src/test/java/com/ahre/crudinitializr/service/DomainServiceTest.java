package com.ahre.crudinitializr.service;

import com.ahre.crudinitializr.enums.DatabaseEnum;
import com.ahre.crudinitializr.enums.TypeEnum;
import com.ahre.crudinitializr.enums.ValidationEnum;
import com.ahre.crudinitializr.utils.FileUtils;
import com.ahre.crudinitializr.vo.Attribute;
import com.ahre.crudinitializr.vo.Domain;
import com.ahre.crudinitializr.vo.Entity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainServiceTest {

    private final FileUtils fileUtils = new FileUtils();
    private final DomainService domainService = new DomainService(
            fileUtils,
            "/src/main/java/com/example/projectBase",
            "/src/test/java/com/example/projectBase",
            "import com.example.projectBase");
    private final List<Path> generatedProjects = new ArrayList<>();

    @AfterEach
    void cleanGeneratedProjects() {
        generatedProjects.forEach(fileUtils::deleteQuietly);
    }

    @Test
    void processDomain_withValidDomain_generatesCompleteProject() throws IOException {
        Path project = track(domainService.processDomain(validDomain()));

        assertTrue(Files.exists(project.resolve("pom.xml")));
        assertTrue(Files.exists(project.resolve(".gitignore")));
        assertTrue(Files.exists(project.resolve("src/main/java/com/example/projectBase/ProjectBaseApplication.java")));
        assertTrue(Files.exists(project.resolve("src/main/java/com/example/projectBase/domain/Persona.java")));
        assertTrue(Files.exists(project.resolve("src/main/java/com/example/projectBase/domain/Pedido.java")));
        assertTrue(Files.exists(project.resolve("src/test/java/com/example/projectBase/tests/service/PersonaServiceTest.java")));
        assertFalse(Files.exists(project.resolve("src/main/java/com/example/projectBase/domain/DomainBase.java")));
        assertFalse(Files.exists(project.resolve("src/main/java/com/example/projectBase/service/ServiceBase.java")));
    }

    @Test
    void processDomain_withTitle_generatesPomWithProjectName() throws IOException {
        Path project = track(domainService.processDomain(validDomain()));
        String pom = Files.readString(project.resolve("pom.xml"));

        assertTrue(pom.contains("<artifactId>mi-proyecto</artifactId>"));
        assertTrue(pom.contains("<name>Mi Proyecto</name>"));
        assertTrue(pom.contains("<description>Mi Proyecto</description>"));
        assertFalse(pom.contains("PROJECT_"));
    }

    @Test
    void processDomain_withCustomRelations_generatesMatchingJpaAndSqlColumns() throws IOException {
        Domain domain = validDomain();
        domain.setDatabase(DatabaseEnum.MYSQL);

        Path project = track(domainService.processDomain(domain));

        String pedido = Files.readString(project.resolve("src/main/java/com/example/projectBase/domain/Pedido.java"));
        assertTrue(pedido.contains("private Persona cliente;"));
        assertTrue(pedido.contains("@JoinColumn(name = \"pedido_item_id\")"));
        assertTrue(pedido.contains("private List<Persona> items;"));

        String schema = Files.readString(project.resolve("doc/scripts/schema.sql"));
        assertTrue(schema.contains("`cliente_id` BIGINT"));
        assertTrue(schema.contains("`pedido_item_id` BIGINT"));
        assertTrue(schema.contains("`fecha_creacion` TIMESTAMP"));
        assertTrue(schema.contains("ALTER TABLE `pedido` ADD CONSTRAINT `fk_pedido_"));
        assertTrue(schema.contains("ALTER TABLE `persona` ADD CONSTRAINT `fk_persona_"));
        assertTrue(schema.contains("REFERENCES `persona`(`id`)"));
        assertTrue(schema.contains("REFERENCES `pedido`(`id`)"));
        assertTrue(schema.indexOf("ALTER TABLE") > schema.lastIndexOf("CREATE TABLE"));

        String testSchema = Files.readString(project.resolve("src/test/resources/schema.sql"));
        assertTrue(testSchema.contains("DROP TABLE IF EXISTS \"pedido\" CASCADE;"));
        assertTrue(testSchema.contains("\"pedido_item_id\" BIGINT"));
        assertTrue(testSchema.contains("REFERENCES \"pedido\"(\"id\")"));
    }

    @Test
    void processDomain_withReservedWords_quotesIdentifiersByDialect() throws IOException {
        Domain domain = validDomain();
        domain.setDatabase(DatabaseEnum.MYSQL);
        Entity order = entity("order");
        order.getAttributes().put("total", attribute(TypeEnum.INTEGER, null));
        domain.getEntities().add(order);

        Path project = track(domainService.processDomain(domain));

        String schema = Files.readString(project.resolve("doc/scripts/schema.sql"));
        assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS `order`("));
        assertTrue(schema.contains("`total` INT"));

        String testSchema = Files.readString(project.resolve("src/test/resources/schema.sql"));
        assertTrue(testSchema.contains("CREATE TABLE IF NOT EXISTS \"order\"("));
        assertTrue(testSchema.contains("\"total\" INT"));
    }

    @Test
    void processDomain_withTwoListsWithSameName_generatesPrefixedColumns() throws IOException {
        Domain domain = validDomain();
        domain.setDatabase(DatabaseEnum.MYSQL);
        Entity alfa = entity("alfa");
        alfa.getAttributes().put("item", attribute(TypeEnum.CUSTOM_LIST, "persona"));
        Entity beta = entity("beta");
        beta.getAttributes().put("item", attribute(TypeEnum.CUSTOM_LIST, "persona"));
        domain.getEntities().addAll(List.of(alfa, beta));

        Path project = track(domainService.processDomain(domain));

        String schema = Files.readString(project.resolve("doc/scripts/schema.sql"));
        assertTrue(schema.contains("`alfa_item_id` BIGINT"));
        assertTrue(schema.contains("`beta_item_id` BIGINT"));
        assertTrue(schema.contains("REFERENCES `alfa`(`id`)"));
        assertTrue(schema.contains("REFERENCES `beta`(`id`)"));
    }

    @Test
    void processDomain_withStringNotBlank_generatesIsBlankValidation() throws IOException {
        Path project = track(domainService.processDomain(validDomain()));
        String service = Files.readString(project.resolve("src/main/java/com/example/projectBase/service/PersonaService.java"));

        assertTrue(service.contains("Assert.isTrue(persona.getNombre() != null && !persona.getNombre().isBlank(), \"nombre can not be blank\");"));
    }

    @Test
    void processDomain_withOptionalCustomRelation_generatesNullGuard() throws IOException {
        Path project = track(domainService.processDomain(validDomain()));
        String service = Files.readString(project.resolve("src/main/java/com/example/projectBase/service/PedidoService.java"));

        assertTrue(service.contains("if (pedido.getCliente() != null) {"));
        assertTrue(service.contains("personaService.save(pedido.getCliente());"));
    }

    @Test
    void processDomain_withGeneratedTest_generatesValidImports() throws IOException {
        Path project = track(domainService.processDomain(validDomain()));
        String test = Files.readString(project.resolve("src/test/java/com/example/projectBase/tests/service/PersonaServiceTest.java"));

        assertTrue(test.contains("import com.example.projectBase.service.PersonaService;"));
        assertTrue(test.contains("import com.example.projectBase.domain.Persona;"));
        assertTrue(test.contains("import com.example.projectBase.builder.PersonaBuilder;"));
    }

    @Test
    void processDomain_withTwoRelationsToSameEntity_generatesSingleServiceDependency() throws IOException {
        Domain domain = validDomain();
        domain.getEntities().get(1).getAttributes().put("vendedor",
                attribute(TypeEnum.CUSTOM, "persona"));

        Path project = track(domainService.processDomain(domain));
        String service = Files.readString(project.resolve("src/main/java/com/example/projectBase/service/PedidoService.java"));

        int firstOccurrence = service.indexOf("private final PersonaService personaService;");
        int lastOccurrence = service.lastIndexOf("private final PersonaService personaService;");
        assertTrue(firstOccurrence >= 0);
        assertEquals(firstOccurrence, lastOccurrence);
        assertTrue(service.contains("personaService.save(pedido.getVendedor());"));
    }

    @Test
    void processDomain_withJavaKeywordAsAttributeName_throwsException() {
        Domain domain = validDomain();
        domain.getEntities().get(0).getAttributes().put("class", attribute(TypeEnum.STRING, null));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("no es válido"));
    }

    @Test
    void processDomain_withScalarAttributes_generatesBuilderDefaultsAndDeleteGuard() throws IOException {
        Domain domain = validDomain();
        Entity persona = domain.getEntities().get(0);
        persona.getAttributes().put("edad", attribute(TypeEnum.INTEGER, null));
        persona.getAttributes().put("total", attribute(TypeEnum.LONG, null));
        persona.getAttributes().put("activo", attribute(TypeEnum.BOOLEAN, null));

        Path project = track(domainService.processDomain(domain));

        String builder = Files.readString(project.resolve("src/test/java/com/example/projectBase/builder/PersonaBuilder.java"));
        assertTrue(builder.contains("builder.withNombre(\"test\");"));
        assertTrue(builder.contains("builder.withEdad(1);"));
        assertTrue(builder.contains("builder.withTotal(1L);"));
        assertTrue(builder.contains("builder.withActivo(true);"));

        String service = Files.readString(project.resolve("src/main/java/com/example/projectBase/service/PersonaService.java"));
        assertTrue(service.contains("if (!personaRepository.existsById(id))"));
        assertFalse(service.contains("EmptyResultDataAccessException"));
    }

    @Test
    void processDomain_withReservedColumnAttribute_throwsException() {
        Domain domain = validDomain();
        domain.getEntities().get(0).getAttributes().put("id", attribute(TypeEnum.INTEGER, null));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("reservadas"));
    }

    @Test
    void processDomain_withReservedTimestampAttribute_throwsException() {
        Domain domain = validDomain();
        domain.getEntities().get(0).getAttributes().put("fechaCreacion", attribute(TypeEnum.STRING, null));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("reservadas"));
    }

    @Test
    void processDomain_withSnakeCaseAttributeCollision_throwsException() {
        Domain domain = validDomain();
        Entity persona = domain.getEntities().get(0);
        persona.getAttributes().put("fooBar", attribute(TypeEnum.STRING, null));
        persona.getAttributes().put("foo_bar", attribute(TypeEnum.STRING, null));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("columna duplicada"));
    }

    @Test
    void processDomain_withSnakeCaseEntityCollision_throwsException() {
        Domain domain = validDomain();
        domain.getEntities().add(entity("fooBar"));
        domain.getEntities().add(entity("foo_bar"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("tabla repetida"));
    }

    @Test
    void processDomain_withSchemaColumnCollision_throwsException() {
        Domain domain = validDomain();
        domain.getEntities().get(0).getAttributes().put("alfaItem", attribute(TypeEnum.CUSTOM, "alfa"));
        Entity alfa = entity("alfa");
        alfa.getAttributes().put("item", attribute(TypeEnum.CUSTOM_LIST, "persona"));
        domain.getEntities().add(alfa);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("columna duplicada"));
    }

    @Test
    void processDomain_withInvalidEntityName_throwsException() {
        Domain domain = validDomain();
        domain.getEntities().get(0).setEntityName("mi entidad");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("no es válido"));
    }

    @Test
    void processDomain_withDuplicateEntityNames_throwsException() {
        Domain domain = validDomain();
        domain.getEntities().get(1).setEntityName("Persona");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("está repetido"));
    }

    @Test
    void processDomain_withUnknownCustomType_throwsException() {
        Domain domain = validDomain();
        domain.getEntities().get(1).getAttributes().get("cliente").setCustomType("inexistente");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("entidad inexistente"));
    }

    @Test
    void processDomain_withSelfReference_throwsException() {
        Domain domain = validDomain();
        domain.getEntities().get(1).getAttributes().put("dependencia",
                attribute(TypeEnum.CUSTOM, "pedido"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("no puede referenciarse a sí mismo"));
    }

    @Test
    void processDomain_withValidationNotAllowedForType_throwsException() {
        Domain domain = validDomain();
        domain.getEntities().get(0).getAttributes().put("edad",
                attribute(TypeEnum.INTEGER, null, ValidationEnum.NOT_BLANK));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("no es válida"));
    }

    @Test
    void processDomain_withoutEntities_throwsException() {
        Domain domain = validDomain();
        domain.setEntities(new ArrayList<>());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("al menos una entidad"));
    }

    @Test
    void processDomain_withBlankTitle_throwsException() {
        Domain domain = validDomain();
        domain.setTitle("   ");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("nombre del proyecto"));
    }

    @Test
    void processDomain_withDuplicateAttributeNames_throwsException() {
        Domain domain = validDomain();
        Entity persona = domain.getEntities().get(0);
        persona.getAttributes().put("Nombre", attribute(TypeEnum.STRING, null));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("está repetido"));
    }

    @Test
    void processDomain_withoutDatabase_generatesPostgresqlConfiguration() throws IOException {
        Domain domain = validDomain();
        domain.setDatabase(null);

        Path project = track(domainService.processDomain(domain));

        String pom = Files.readString(project.resolve("pom.xml"));
        assertTrue(pom.contains("<groupId>org.postgresql</groupId>"));
        assertTrue(pom.contains("<artifactId>postgresql</artifactId>"));
        assertTrue(pom.contains("<artifactId>h2</artifactId>"));
        assertFalse(pom.contains("mysql-connector-j"));
        assertFalse(pom.contains("mariadb-java-client"));
        assertFalse(pom.contains("hsqldb"));
        assertFalse(pom.contains("DATABASE_DEPENDENCY"));
        assertFalse(pom.contains("TEST_H2_DEPENDENCY"));

        String properties = Files.readString(project.resolve("src/main/resources/application.properties"));
        assertTrue(properties.contains("# Base de datos: PostgreSQL"));
        assertTrue(properties.contains("${DB_URL:jdbc:postgresql://localhost:5432/test}"));
        assertFalse(properties.contains("spring.h2.console.enabled"));

        String testProperties = Files.readString(project.resolve("src/test/resources/application.properties"));
        assertTrue(testProperties.contains("jdbc:h2:mem:testdb"));

        String compose = Files.readString(project.resolve("docker-compose.yml"));
        assertTrue(compose.contains("image: postgres:17"));

        String readme = Files.readString(project.resolve("README.md"));
        assertTrue(readme.contains("**PostgreSQL**"));
        assertTrue(readme.contains("docker compose up -d"));
    }

    @Test
    void processDomain_withMariadb_generatesMariadbDriverAndMysqlLikeSchema() throws IOException {
        Domain domain = validDomain();
        domain.setDatabase(DatabaseEnum.MARIADB);

        Path project = track(domainService.processDomain(domain));

        String pom = Files.readString(project.resolve("pom.xml"));
        assertTrue(pom.contains("<groupId>org.mariadb.jdbc</groupId>"));
        assertTrue(pom.contains("<artifactId>mariadb-java-client</artifactId>"));
        assertFalse(pom.contains("mysql-connector-j"));

        String properties = Files.readString(project.resolve("src/main/resources/application.properties"));
        assertTrue(properties.contains("# Base de datos: MariaDB"));
        assertTrue(properties.contains("${DB_URL:jdbc:mariadb://localhost:3306/test}"));

        String schema = Files.readString(project.resolve("doc/scripts/schema.sql"));
        assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS `pedido`("));
        assertTrue(schema.contains("`id` BIGINT AUTO_INCREMENT PRIMARY KEY"));
        assertTrue(schema.contains("ALTER TABLE `pedido` ADD CONSTRAINT"));

        String compose = Files.readString(project.resolve("docker-compose.yml"));
        assertTrue(compose.contains("image: mariadb:11"));
    }

    @Test
    void processDomain_withPostgresql_generatesPostgresDriverPropertiesAndSchema() throws IOException {
        Domain domain = validDomain();
        domain.setDatabase(DatabaseEnum.POSTGRESQL);

        Path project = track(domainService.processDomain(domain));

        String pom = Files.readString(project.resolve("pom.xml"));
        assertTrue(pom.contains("<groupId>org.postgresql</groupId>"));
        assertTrue(pom.contains("<artifactId>postgresql</artifactId>"));
        assertFalse(pom.contains("mysql-connector-j"));

        String properties = Files.readString(project.resolve("src/main/resources/application.properties"));
        assertTrue(properties.contains("# Base de datos: PostgreSQL"));
        assertTrue(properties.contains("${DB_URL:jdbc:postgresql://localhost:5432/test}"));

        String schema = Files.readString(project.resolve("doc/scripts/schema.sql"));
        assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS \"pedido\"("));
        assertTrue(schema.contains("\"id\" BIGSERIAL PRIMARY KEY"));
        assertTrue(schema.contains("ALTER TABLE \"pedido\" ADD CONSTRAINT"));
        assertTrue(schema.contains("REFERENCES \"persona\"(\"id\")"));

        String testSchema = Files.readString(project.resolve("src/test/resources/schema.sql"));
        assertTrue(testSchema.contains("\"id\" BIGINT AUTO_INCREMENT PRIMARY KEY"));
        assertTrue(testSchema.contains("DROP TABLE IF EXISTS \"pedido\" CASCADE;"));

        String compose = Files.readString(project.resolve("docker-compose.yml"));
        assertTrue(compose.contains("image: postgres:17"));
        assertTrue(compose.contains("POSTGRES_DB: test"));

        String readme = Files.readString(project.resolve("README.md"));
        assertTrue(readme.contains("**PostgreSQL**"));
        assertTrue(readme.contains("docker compose up -d"));
    }

    @Test
    void processDomain_withH2_generatesEmbeddedConfigurationWithoutDockerCompose() throws IOException {
        Domain domain = validDomain();
        domain.setDatabase(DatabaseEnum.H2);

        Path project = track(domainService.processDomain(domain));

        String pom = Files.readString(project.resolve("pom.xml"));
        assertTrue(pom.contains("<groupId>com.h2database</groupId>"));
        assertTrue(pom.contains("<artifactId>h2</artifactId>"));
        assertEquals(1, countOccurrences(pom, "<artifactId>h2</artifactId>"));
        assertFalse(pom.contains("mysql-connector-j"));

        String properties = Files.readString(project.resolve("src/main/resources/application.properties"));
        assertTrue(properties.contains("# Base de datos: H2"));
        assertTrue(properties.contains("${DB_URL:jdbc:h2:file:./data/test;DB_CLOSE_ON_EXIT=FALSE}"));
        assertTrue(properties.contains("${DB_USER:sa}"));
        assertTrue(properties.contains("spring.h2.console.enabled=true"));

        String schema = Files.readString(project.resolve("doc/scripts/schema.sql"));
        assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS \"pedido\"("));
        assertTrue(schema.contains("\"id\" BIGINT AUTO_INCREMENT PRIMARY KEY"));

        assertFalse(Files.exists(project.resolve("docker-compose.yml")));

        String readme = Files.readString(project.resolve("README.md"));
        assertTrue(readme.contains("H2 (embebida)"));
        assertTrue(readme.contains("h2-console"));
    }

    @Test
    void processDomain_withSqlite_generatesDriverCommunityDialectAndInlineForeignKeys() throws IOException {
        Domain domain = validDomain();
        domain.setDatabase(DatabaseEnum.SQLITE);

        Path project = track(domainService.processDomain(domain));

        String pom = Files.readString(project.resolve("pom.xml"));
        assertTrue(pom.contains("<groupId>org.xerial</groupId>"));
        assertTrue(pom.contains("<artifactId>sqlite-jdbc</artifactId>"));
        assertTrue(pom.contains("<groupId>org.hibernate.orm</groupId>"));
        assertTrue(pom.contains("<artifactId>hibernate-community-dialects</artifactId>"));
        assertTrue(pom.contains("<artifactId>h2</artifactId>"));
        assertFalse(pom.contains("mysql-connector-j"));
        assertFalse(pom.contains("mariadb-java-client"));
        assertFalse(pom.contains("org.postgresql"));

        String properties = Files.readString(project.resolve("src/main/resources/application.properties"));
        assertTrue(properties.contains("# Base de datos: SQLite"));
        assertTrue(properties.contains("${DB_URL:jdbc:sqlite:./data/test.db?foreign_keys=on}"));
        assertTrue(properties.contains("spring.datasource.driver-class-name=org.sqlite.JDBC"));
        assertTrue(properties.contains("spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect"));
        assertFalse(properties.contains("spring.h2.console.enabled"));

        String schema = Files.readString(project.resolve("doc/scripts/schema.sql"));
        assertTrue(schema.contains("PRAGMA foreign_keys = ON;"));
        assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS \"pedido\"("));
        assertTrue(schema.contains("\"id\" INTEGER PRIMARY KEY AUTOINCREMENT"));
        assertTrue(schema.contains("CONSTRAINT \"fk_pedido_"));
        assertTrue(schema.contains("FOREIGN KEY (\"cliente_id\") REFERENCES \"persona\"(\"id\")"));
        assertTrue(schema.contains("CONSTRAINT \"fk_persona_"));
        assertTrue(schema.contains("FOREIGN KEY (\"pedido_item_id\") REFERENCES \"pedido\"(\"id\")"));
        assertFalse(schema.contains("ALTER TABLE"));

        String testSchema = Files.readString(project.resolve("src/test/resources/schema.sql"));
        assertTrue(testSchema.contains("\"id\" BIGINT AUTO_INCREMENT PRIMARY KEY"));
        assertTrue(testSchema.contains("ALTER TABLE \"pedido\" ADD CONSTRAINT"));

        assertFalse(Files.exists(project.resolve("docker-compose.yml")));

        String readme = Files.readString(project.resolve("README.md"));
        assertTrue(readme.contains("**SQLite**"));
        assertTrue(readme.contains("SQLite (archivo)"));
        assertTrue(readme.contains("foreign_keys=on"));
    }

    private long countOccurrences(String value, String token) {
        return value.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }

    private Domain validDomain() {
        Domain domain = new Domain();
        domain.setTitle("Mi Proyecto");

        Entity persona = entity("persona");
        persona.getAttributes().put("nombre",
                attribute(TypeEnum.STRING, null, ValidationEnum.NOT_BLANK));

        Entity pedido = entity("pedido");
        pedido.getAttributes().put("cliente", attribute(TypeEnum.CUSTOM, "persona"));
        pedido.getAttributes().put("item", attribute(TypeEnum.CUSTOM_LIST, "persona"));

        domain.setEntities(new ArrayList<>(List.of(persona, pedido)));
        return domain;
    }

    private Entity entity(String name) {
        Entity entity = new Entity();
        entity.setEntityName(name);
        entity.setAttributes(new HashMap<>());
        return entity;
    }

    private Attribute attribute(TypeEnum type, String customType, ValidationEnum... validations) {
        Attribute attribute = new Attribute();
        attribute.setType(type);
        attribute.setCustomType(customType);
        attribute.setValidations(new ArrayList<>(List.of(validations)));
        return attribute;
    }

    private Path track(Path project) {
        generatedProjects.add(project);
        return project;
    }
}
