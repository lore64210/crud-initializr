package com.ahre.crudinitializr.service;

import com.ahre.crudinitializr.enums.ApiEnum;
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
import java.math.BigDecimal;
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
        assertTrue(Files.exists(project.resolve("src/main/java/com/example/projectBase/MiProyectoApplication.java")));
        assertTrue(Files.exists(project.resolve("src/main/java/com/example/projectBase/domain/Persona.java")));
        assertTrue(Files.exists(project.resolve("src/main/java/com/example/projectBase/domain/Pedido.java")));
        assertTrue(Files.exists(project.resolve("src/test/java/com/example/projectBase/tests/service/PersonaServiceTest.java")));
        String mainClass = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/MiProyectoApplication.java"));
        assertTrue(mainClass.contains("public class MiProyectoApplication"));
        assertTrue(mainClass.contains("SpringApplication.run(MiProyectoApplication.class, args)"));
        assertFalse(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/ProjectBaseApplication.java")));
        assertFalse(Files.exists(project.resolve("src/main/java/com/example/projectBase/domain/DomainBase.java")));
        assertFalse(Files.exists(project.resolve("src/main/java/com/example/projectBase/service/ServiceBase.java")));
    }

    @Test
    void processDomain_withNumericTitle_generatesSafeMainClassAndDatabaseName() throws IOException {
        Domain domain = validDomain();
        domain.setTitle("123 Tienda Online");

        Path project = track(domainService.processDomain(domain));

        assertTrue(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/Project123TiendaOnlineApplication.java")));
        String properties = Files.readString(
                project.resolve("src/main/resources/application.properties"));
        assertTrue(properties.contains(
                "${DB_URL:jdbc:postgresql://localhost:5432/db_123_tienda_online}"));
        String compose = Files.readString(project.resolve("docker-compose.yml"));
        assertTrue(compose.contains("POSTGRES_DB: db_123_tienda_online"));
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

        assertTrue(service.contains("Persona clienteValue = pedido.getCliente();"));
        assertTrue(service.contains("if (clienteValue != null && clienteValue.getId() != null) {"));
        assertTrue(service.contains("pedido.setCliente(personaRepository.findById(clienteValue.getId())"));
    }

    @Test
    void processDomain_withRelations_generatesRequestDtoMapperAndRelationAttach() throws IOException {
        Path project = track(domainService.processDomain(validDomain()));

        String request = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/dto/PedidoRequest.java"));
        assertTrue(request.contains("private Long clienteId;"));
        assertTrue(request.contains("private List<Long> itemsIds;"));

        String mapper = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/mapper/PedidoMapper.java"));
        assertTrue(mapper.contains("if (request.getClienteId() != null)"));
        assertTrue(mapper.contains("clienteRef.setId(request.getClienteId());"));
        assertTrue(mapper.contains("if (request.getItemsIds() != null)"));
        assertTrue(mapper.contains("entity.setItems(itemsRef);"));

        String service = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/service/PedidoService.java"));
        assertTrue(service.contains("@Transactional"));
        assertTrue(service.contains("attachRelations(pedido);"));
        assertTrue(service.contains("private final PersonaRepository personaRepository;"));
        assertTrue(service.contains("pedido.setCliente(personaRepository.findById(clienteValue.getId())"));
        assertFalse(service.contains("personaService.save"));

        String controller = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/controller/PedidoController.java"));
        assertTrue(controller.contains("@RequestBody PedidoRequest request"));
        assertTrue(controller.contains("PedidoMapper.toEntity(request)"));
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

        int firstOccurrence = service.indexOf("private final PersonaRepository personaRepository;");
        int lastOccurrence = service.lastIndexOf("private final PersonaRepository personaRepository;");
        assertTrue(firstOccurrence >= 0);
        assertEquals(firstOccurrence, lastOccurrence);
        assertTrue(service.contains("pedido.setCliente(personaRepository.findById(clienteValue.getId())"));
        assertTrue(service.contains("pedido.setVendedor(personaRepository.findById(vendedorValue.getId())"));
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
    void processDomain_withRelations_generatesBuildersWithRelatedInstances() throws IOException {
        Domain domain = validDomain();

        Path project = track(domainService.processDomain(domain));

        String builder = Files.readString(project.resolve(
                "src/test/java/com/example/projectBase/builder/PedidoBuilder.java"));
        assertTrue(builder.contains("public static PedidoBuilder generic()"));
        assertTrue(builder.contains("static PedidoBuilder generic(int depth)"));
        assertTrue(builder.contains("if (depth > 0) {"));
        assertTrue(builder.contains("builder.withCliente(PersonaBuilder.generic(depth - 1).build());"));
        assertTrue(builder.contains(
                "builder.withItems(new ArrayList<>(List.of(PersonaBuilder.generic(depth - 1).build())));"));
        assertTrue(builder.contains("import java.util.ArrayList;"));
        assertTrue(builder.contains("import java.util.List;"));
        assertFalse(builder.contains("withCliente(null)"));
        assertFalse(builder.contains("withItems(null)"));

        String personaBuilder = Files.readString(project.resolve(
                "src/test/java/com/example/projectBase/builder/PersonaBuilder.java"));
        assertTrue(personaBuilder.contains("static PersonaBuilder generic(int depth)"));
        assertFalse(personaBuilder.contains("if (depth > 0) {"));
    }

    @Test
    void processDomain_withNewScalarTypes_generatesJavaTypesSchemaAndValidations() throws IOException {
        Domain domain = validDomain();
        domain.setApiType(ApiEnum.GRAPHQL);
        Entity persona = domain.getEntities().get(0);

        persona.getAttributes().put("fechaNacimiento",
                attribute(TypeEnum.DATE, null, ValidationEnum.PAST));
        persona.getAttributes().put("creadoEn",
                attribute(TypeEnum.DATETIME, null, ValidationEnum.FUTURE_OR_PRESENT));
        persona.getAttributes().put("horaInicio",
                attribute(TypeEnum.TIME, null, ValidationEnum.PAST));
        Attribute precio = attribute(TypeEnum.DECIMAL, null,
                ValidationEnum.POSITIVE, ValidationEnum.MIN, ValidationEnum.MAX);
        precio.setMinValue(new BigDecimal("1.5"));
        precio.setMaxValue(new BigDecimal("999.99"));
        persona.getAttributes().put("precio", precio);
        persona.getAttributes().put("peso",
                attribute(TypeEnum.DOUBLE, null, ValidationEnum.POSITIVE));
        persona.getAttributes().put("codigo",
                attribute(TypeEnum.UUID, null, ValidationEnum.NOT_NULL));

        Path project = track(domainService.processDomain(domain));

        String personaFile = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/domain/Persona.java"));
        assertTrue(personaFile.contains("import java.time.LocalDate;"));
        assertTrue(personaFile.contains("import java.time.LocalDateTime;"));
        assertTrue(personaFile.contains("import java.time.LocalTime;"));
        assertTrue(personaFile.contains("import java.math.BigDecimal;"));
        assertTrue(personaFile.contains("import java.util.UUID;"));
        assertTrue(personaFile.contains("private LocalDate fechaNacimiento;"));
        assertTrue(personaFile.contains("private LocalDateTime creadoEn;"));
        assertTrue(personaFile.contains("private LocalTime horaInicio;"));
        assertTrue(personaFile.contains("private BigDecimal precio;"));
        assertTrue(personaFile.contains("private Double peso;"));
        assertTrue(personaFile.contains("private UUID codigo;"));

        String service = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/service/PersonaService.java"));
        assertTrue(service.contains(
                "Assert.isTrue(persona.getFechaNacimiento() == null || persona.getFechaNacimiento().isBefore(java.time.LocalDate.now()), \"fechaNacimiento must be in the past\");"));
        assertTrue(service.contains(
                "Assert.isTrue(persona.getCreadoEn() == null || !persona.getCreadoEn().isBefore(java.time.LocalDateTime.now()), \"creadoEn must be in the future or present\");"));
        assertTrue(service.contains(
                "Assert.isTrue(persona.getHoraInicio() == null || persona.getHoraInicio().isBefore(java.time.LocalTime.now()), \"horaInicio must be in the past\");"));
        assertTrue(service.contains(
                "Assert.isTrue(persona.getPrecio() == null || persona.getPrecio().compareTo(java.math.BigDecimal.ZERO) > 0, \"precio must be positive\");"));
        assertTrue(service.contains(
                "Assert.isTrue(persona.getPrecio() == null || persona.getPrecio().compareTo(new java.math.BigDecimal(\"1.5\")) >= 0, \"precio must be greater than or equal to 1.5\");"));
        assertTrue(service.contains(
                "Assert.isTrue(persona.getPrecio() == null || persona.getPrecio().compareTo(new java.math.BigDecimal(\"999.99\")) <= 0, \"precio must be less than or equal to 999.99\");"));
        assertTrue(service.contains(
                "Assert.isTrue(persona.getPeso() == null || persona.getPeso() > 0, \"peso must be positive\");"));
        assertTrue(service.contains(
                "Assert.notNull(persona.getCodigo(), \"codigo can not be null\");"));

        String builder = Files.readString(project.resolve(
                "src/test/java/com/example/projectBase/builder/PersonaBuilder.java"));
        assertTrue(builder.contains("withFechaNacimiento(LocalDate val)"));
        assertTrue(builder.contains("builder.withFechaNacimiento(LocalDate.now().minusDays(1));"));
        assertTrue(builder.contains("builder.withCreadoEn(LocalDateTime.now().plusDays(1));"));
        assertTrue(builder.contains("builder.withHoraInicio(LocalTime.MIN);"));
        assertTrue(builder.contains("builder.withPrecio(new BigDecimal(\"1.5\"));"));
        assertTrue(builder.contains("builder.withPeso(1.0);"));
        assertTrue(builder.contains(
                "builder.withCodigo(UUID.fromString(\"00000000-0000-0000-0000-000000000001\"));"));

        String schema = Files.readString(project.resolve("doc/scripts/schema.sql"));
        assertTrue(schema.contains("\"fecha_nacimiento\" DATE"));
        assertTrue(schema.contains("\"creado_en\" TIMESTAMP"));
        assertTrue(schema.contains("\"hora_inicio\" TIME"));
        assertTrue(schema.contains("\"precio\" DECIMAL(19,4)"));
        assertTrue(schema.contains("\"peso\" DOUBLE PRECISION"));
        assertTrue(schema.contains("\"codigo\" UUID"));

        String graphqlSchema = Files.readString(project.resolve(
                "src/main/resources/graphql/schema.graphqls"));
        assertTrue(graphqlSchema.contains("fechaCreacion: DateTime"));
        assertTrue(graphqlSchema.contains("fechaNacimiento: Date"));
        assertTrue(graphqlSchema.contains("creadoEn: DateTime"));
        assertTrue(graphqlSchema.contains("horaInicio: Time"));
        assertTrue(graphqlSchema.contains("precio: BigDecimal"));
        assertTrue(graphqlSchema.contains("peso: Float"));
        assertTrue(graphqlSchema.contains("codigo: UUID"));
    }

    @Test
    void processDomain_withNewScalarTypes_usesDialectSpecificSqlTypes() throws IOException {
        Domain domain = validDomain();
        domain.setDatabase(DatabaseEnum.MYSQL);
        Entity persona = domain.getEntities().get(0);
        persona.getAttributes().put("fechaNacimiento", attribute(TypeEnum.DATE, null));
        persona.getAttributes().put("creadoEn", attribute(TypeEnum.DATETIME, null));
        persona.getAttributes().put("horaInicio", attribute(TypeEnum.TIME, null));
        persona.getAttributes().put("precio", attribute(TypeEnum.DECIMAL, null));
        persona.getAttributes().put("peso", attribute(TypeEnum.DOUBLE, null));
        persona.getAttributes().put("codigo", attribute(TypeEnum.UUID, null));

        Path mysqlProject = track(domainService.processDomain(domain));
        String mysqlSchema = Files.readString(mysqlProject.resolve("doc/scripts/schema.sql"));
        assertTrue(mysqlSchema.contains("`fecha_nacimiento` DATE"));
        assertTrue(mysqlSchema.contains("`creado_en` DATETIME"));
        assertTrue(mysqlSchema.contains("`hora_inicio` TIME"));
        assertTrue(mysqlSchema.contains("`precio` DECIMAL(19,4)"));
        assertTrue(mysqlSchema.contains("`peso` DOUBLE"));
        assertTrue(mysqlSchema.contains("`codigo` CHAR(36)"));

        Domain sqliteDomain = validDomain();
        sqliteDomain.setDatabase(DatabaseEnum.SQLITE);
        Entity sqlitePersona = sqliteDomain.getEntities().get(0);
        sqlitePersona.getAttributes().put("creadoEn", attribute(TypeEnum.DATETIME, null));
        sqlitePersona.getAttributes().put("peso", attribute(TypeEnum.DOUBLE, null));
        sqlitePersona.getAttributes().put("codigo", attribute(TypeEnum.UUID, null));

        Path sqliteProject = track(domainService.processDomain(sqliteDomain));
        String sqliteSchema = Files.readString(sqliteProject.resolve("doc/scripts/schema.sql"));
        assertTrue(sqliteSchema.contains("\"creado_en\" TIMESTAMP"));
        assertTrue(sqliteSchema.contains("\"peso\" REAL"));
        assertTrue(sqliteSchema.contains("\"codigo\" TEXT"));
    }

    @Test
    void processDomain_withTemporalValidationOnString_throwsException() {
        Domain domain = validDomain();
        domain.getEntities().get(0).getAttributes().put("nombre",
                attribute(TypeEnum.STRING, null, ValidationEnum.PAST));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("no es válida"));
    }

    @Test
    void processDomain_withNumericValidationOnDate_throwsException() {
        Domain domain = validDomain();
        domain.getEntities().get(0).getAttributes().put("fechaNacimiento",
                attribute(TypeEnum.DATE, null, ValidationEnum.POSITIVE));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("no es válida"));
    }

    @Test
    void processDomain_withMinValidationWithoutValue_throwsException() {
        Domain domain = validDomain();
        domain.getEntities().get(0).getAttributes().put("edad",
                attribute(TypeEnum.INTEGER, null, ValidationEnum.MIN));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("valor mínimo"));
    }

    @Test
    void processDomain_withFractionalIntegerMin_throwsException() {
        Domain domain = validDomain();
        Attribute edad = attribute(TypeEnum.INTEGER, null, ValidationEnum.MIN);
        edad.setMinValue(new BigDecimal("1.5"));
        domain.getEntities().get(0).getAttributes().put("edad", edad);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("número entero"));
    }

    @Test
    void processDomain_withMinGreaterThanMax_throwsException() {
        Domain domain = validDomain();
        Attribute edad = attribute(TypeEnum.INTEGER, null, ValidationEnum.MIN, ValidationEnum.MAX);
        edad.setMinValue(new BigDecimal("10"));
        edad.setMaxValue(new BigDecimal("5"));
        domain.getEntities().get(0).getAttributes().put("edad", edad);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("no puede ser mayor"));
    }

    @Test
    void processDomain_withContradictoryNumericValidations_throwsException() {
        Domain domain = validDomain();
        Attribute edad = attribute(TypeEnum.INTEGER, null,
                ValidationEnum.POSITIVE, ValidationEnum.NEGATIVE);
        domain.getEntities().get(0).getAttributes().put("edad", edad);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("positivo y negativo"));
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
        domain.getEntities().get(0).getAttributes().put("alfaItemId", attribute(TypeEnum.STRING, null));
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
        assertFalse(pom.contains("TEST_SUPPORT_DEPENDENCY"));

        String properties = Files.readString(project.resolve("src/main/resources/application.properties"));
        assertTrue(properties.contains("# Base de datos: PostgreSQL"));
        assertTrue(properties.contains("${DB_URL:jdbc:postgresql://localhost:5432/mi_proyecto}"));
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
        assertTrue(properties.contains("${DB_URL:jdbc:mariadb://localhost:3306/mi_proyecto}"));

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
        assertTrue(properties.contains("${DB_URL:jdbc:postgresql://localhost:5432/mi_proyecto}"));

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
        assertTrue(compose.contains("POSTGRES_DB: mi_proyecto"));

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
        assertTrue(properties.contains("${DB_URL:jdbc:h2:file:./data/mi_proyecto;DB_CLOSE_ON_EXIT=FALSE}"));
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
        assertTrue(properties.contains("${DB_URL:jdbc:sqlite:./data/mi_proyecto.db?foreign_keys=on}"));
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

    @Test
    void processDomain_withMongodb_generatesDocumentProjectWithTestcontainers() throws IOException {
        Domain domain = flatDomain();
        domain.setDatabase(DatabaseEnum.MONGODB);

        Path project = track(domainService.processDomain(domain));

        String pom = Files.readString(project.resolve("pom.xml"));
        assertTrue(pom.contains("<artifactId>spring-boot-starter-data-mongodb</artifactId>"));
        assertFalse(pom.contains("spring-boot-starter-data-jpa"));
        assertFalse(pom.contains("mysql-connector-j"));
        assertFalse(pom.contains("mariadb-java-client"));
        assertFalse(pom.contains("org.postgresql"));
        assertFalse(pom.contains("<artifactId>h2</artifactId>"));
        assertTrue(pom.contains("<artifactId>spring-boot-testcontainers</artifactId>"));
        assertTrue(pom.contains("<artifactId>testcontainers-mongodb</artifactId>"));
        assertTrue(pom.contains("<artifactId>testcontainers-junit-jupiter</artifactId>"));
        assertTrue(pom.contains("<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>"));
        assertFalse(pom.contains("PERSISTENCE_DEPENDENCY"));
        assertFalse(pom.contains("DATABASE_DEPENDENCY"));
        assertFalse(pom.contains("TEST_SUPPORT_DEPENDENCY"));

        String application = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/MiProyectoApplication.java"));
        assertTrue(application.contains("@EnableMongoAuditing"));

        String persona = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/domain/Persona.java"));
        assertTrue(persona.contains("@Document(collection = \"persona\")"));
        assertTrue(persona.contains("@Id"));
        assertTrue(persona.contains("private String id;"));
        assertTrue(persona.contains("@CreatedDate"));
        assertTrue(persona.contains("private LocalDateTime fechaCreacion;"));
        assertFalse(persona.contains("@Entity"));
        assertFalse(persona.contains("@Table"));
        assertFalse(persona.contains("@ManyToOne"));

        String repository = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/repository/PersonaRepository.java"));
        assertTrue(repository.contains("MongoRepository<Persona, String>"));

        String service = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/service/PersonaService.java"));
        assertTrue(service.contains("findById(String id)"));
        assertTrue(service.contains("delete(String id)"));
        assertFalse(service.contains("CascadeType"));
        assertFalse(service.contains("SAVE_EXTERNAL_ENTITIES"));

        String controller = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/controller/PersonaController.java"));
        assertTrue(controller.contains("@Parameter(description = \"ID de Persona\") String id"));
        assertTrue(controller.contains("@Tag(name = \"Persona\""));
        assertTrue(controller.contains("@Operation(summary = \"Buscar Persona por id\")"));
        assertTrue(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/config/OpenApiConfig.java")));

        String properties = Files.readString(project.resolve("src/main/resources/application.properties"));
        assertTrue(properties.contains("# Base de datos: MongoDB"));
        assertTrue(properties.contains("${MONGODB_URI:mongodb://localhost:27017/mi_proyecto}"));
        assertTrue(properties.contains("spring.data.mongodb.auto-index-creation=true"));
        assertTrue(properties.contains("springdoc.swagger-ui.path=/swagger-ui.html"));
        assertFalse(properties.contains("spring.jpa"));
        assertFalse(properties.contains("spring.datasource"));

        String testBase = Files.readString(project.resolve(
                "src/test/java/com/example/projectBase/tests/ApplicationTests.java"));
        assertTrue(testBase.contains("@Testcontainers"));
        assertTrue(testBase.contains("MongoDBContainer"));
        assertTrue(testBase.contains("mongo:7"));
        assertTrue(testBase.contains("spring.data.mongodb.uri"));

        String serviceTest = Files.readString(project.resolve(
                "src/test/java/com/example/projectBase/tests/service/PersonaServiceTest.java"));
        assertTrue(serviceTest.contains("extends ApplicationTests"));
        assertTrue(serviceTest.contains("personaService.findById(null)"));
        assertFalse(serviceTest.contains("EntityManager"));
        assertFalse(serviceTest.contains("@Transactional"));

        assertFalse(Files.exists(project.resolve("doc/scripts/schema.sql")));
        assertFalse(Files.exists(project.resolve("src/test/resources/schema.sql")));

        String compose = Files.readString(project.resolve("docker-compose.yml"));
        assertTrue(compose.contains("image: mongo:7"));
        assertTrue(compose.contains("MONGO_INITDB_DATABASE: mi_proyecto"));

        String readme = Files.readString(project.resolve("README.md"));
        assertTrue(readme.contains("**MongoDB**"));
        assertTrue(readme.contains("Testcontainers"));
        assertTrue(readme.contains("Docker"));
        assertTrue(readme.contains("documentos embebidos"));
    }

    @Test
    void processDomain_withMongodbAndRelations_throwsClearError() {
        Domain domain = validDomain();
        domain.setDatabase(DatabaseEnum.MONGODB);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("MongoDB"));
        assertTrue(exception.getMessage().contains("relaciones"));
        assertTrue(exception.getMessage().contains("cliente"));
    }

    @Test
    void processDomain_withOneToOneRelation_generatesUniqueForeignKey() throws IOException {
        Domain domain = new Domain();
        domain.setTitle("Mi Proyecto");
        domain.setDatabase(DatabaseEnum.POSTGRESQL);

        Entity persona = entity("persona");
        persona.getAttributes().put("nombre", attribute(TypeEnum.STRING, null));
        persona.getAttributes().put("direccion",
                attribute(TypeEnum.CUSTOM_ONE_TO_ONE, "direccion"));

        Entity direccion = entity("direccion");
        direccion.getAttributes().put("calle", attribute(TypeEnum.STRING, null));

        domain.setEntities(new ArrayList<>(List.of(persona, direccion)));

        Path project = track(domainService.processDomain(domain));

        String personaFile = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/domain/Persona.java"));
        assertTrue(personaFile.contains("@OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})"));
        assertTrue(personaFile.contains("@JoinColumn(name = \"direccion_id\", unique = true)"));
        assertTrue(personaFile.contains("private Direccion direccion;"));

        String schema = Files.readString(project.resolve("doc/scripts/schema.sql"));
        assertTrue(schema.contains("\"direccion_id\" BIGINT UNIQUE"));
        assertTrue(schema.contains(
                "FOREIGN KEY (\"direccion_id\") REFERENCES \"direccion\"(\"id\")"));

        String testSchema = Files.readString(project.resolve("src/test/resources/schema.sql"));
        assertTrue(testSchema.contains("\"direccion_id\" BIGINT UNIQUE"));

        String service = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/service/PersonaService.java"));
        assertTrue(service.contains("Direccion direccionValue = persona.getDireccion();"));
        assertTrue(service.contains("persona.setDireccion(direccionRepository.findById(direccionValue.getId())"));
    }

    @Test
    void processDomain_withManyToManyRelation_generatesJoinTable() throws IOException {
        Domain domain = new Domain();
        domain.setTitle("Mi Proyecto");
        domain.setDatabase(DatabaseEnum.POSTGRESQL);

        Entity persona = entity("persona");
        persona.getAttributes().put("nombre", attribute(TypeEnum.STRING, null));
        persona.getAttributes().put("proyectos",
                attribute(TypeEnum.CUSTOM_MANY_TO_MANY, "proyecto"));

        Entity proyecto = entity("proyecto");
        proyecto.getAttributes().put("titulo", attribute(TypeEnum.STRING, null));

        domain.setEntities(new ArrayList<>(List.of(persona, proyecto)));

        Path project = track(domainService.processDomain(domain));

        String personaFile = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/domain/Persona.java"));
        assertTrue(personaFile.contains("@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})"));
        assertTrue(personaFile.contains("@JoinTable(name = \"persona_proyectos\","));
        assertTrue(personaFile.contains("joinColumns = @JoinColumn(name = \"persona_id\"),"));
        assertTrue(personaFile.contains("inverseJoinColumns = @JoinColumn(name = \"proyecto_id\"))"));
        assertTrue(personaFile.contains("private List<Proyecto> proyectos;"));

        String schema = Files.readString(project.resolve("doc/scripts/schema.sql"));
        assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS \"persona_proyectos\"("));
        assertTrue(schema.contains("\"persona_id\" BIGINT"));
        assertTrue(schema.contains("\"proyecto_id\" BIGINT"));
        assertTrue(schema.contains("PRIMARY KEY (\"persona_id\", \"proyecto_id\")"));
        assertTrue(schema.contains(
                "FOREIGN KEY (\"persona_id\") REFERENCES \"persona\"(\"id\")"));
        assertTrue(schema.contains(
                "FOREIGN KEY (\"proyecto_id\") REFERENCES \"proyecto\"(\"id\")"));

        String testSchema = Files.readString(project.resolve("src/test/resources/schema.sql"));
        assertTrue(testSchema.contains("PRIMARY KEY (\"persona_id\", \"proyecto_id\")"));
    }

    @Test
    void processDomain_withMongodbComposition_generatesNestedDocumentsWithoutCrud() throws IOException {
        Domain domain = mongoCompositionDomain();
        domain.setDatabase(DatabaseEnum.MONGODB);

        Path project = track(domainService.processDomain(domain));

        String pedido = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/domain/Pedido.java"));
        assertTrue(pedido.contains("@Document(collection = \"pedido\")"));
        assertTrue(pedido.contains("private Direccion direccion;"));
        assertTrue(pedido.contains("private List<Item> items;"));
        assertTrue(pedido.contains("import java.util.List;"));
        assertFalse(pedido.contains("@OneToOne"));
        assertFalse(pedido.contains("@ManyToOne"));

        String direccion = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/domain/Direccion.java"));
        assertTrue(direccion.contains("public class Direccion"));
        assertTrue(direccion.contains("private String calle;"));
        assertFalse(direccion.contains("@Document"));
        assertFalse(direccion.contains("@Id"));
        assertFalse(direccion.contains("fechaCreacion"));

        String item = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/domain/Item.java"));
        assertTrue(item.contains("public class Item"));

        assertTrue(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/repository/PedidoRepository.java")));
        assertTrue(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/service/PedidoService.java")));
        assertTrue(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/controller/PedidoController.java")));
        assertTrue(Files.exists(project.resolve(
                "src/test/java/com/example/projectBase/tests/service/PedidoServiceTest.java")));

        assertFalse(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/repository/DireccionRepository.java")));
        assertFalse(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/service/DireccionService.java")));
        assertFalse(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/controller/DireccionController.java")));
        assertFalse(Files.exists(project.resolve(
                "src/test/java/com/example/projectBase/tests/service/DireccionServiceTest.java")));
        assertTrue(Files.exists(project.resolve(
                "src/test/java/com/example/projectBase/builder/DireccionBuilder.java")));

        String direccionBuilder = Files.readString(project.resolve(
                "src/test/java/com/example/projectBase/builder/DireccionBuilder.java"));
        assertTrue(direccionBuilder.contains("static DireccionBuilder generic(int depth)"));
        assertTrue(direccionBuilder.contains("builder.withCalle(\"test\");"));

        String builder = Files.readString(project.resolve(
                "src/test/java/com/example/projectBase/builder/PedidoBuilder.java"));
        assertTrue(builder.contains("withDireccion(Direccion val)"));
        assertTrue(builder.contains("withItems(List<Item> val)"));
        assertTrue(builder.contains("if (depth > 0) {"));
        assertTrue(builder.contains("builder.withDireccion(DireccionBuilder.generic(depth - 1).build());"));
        assertTrue(builder.contains(
                "builder.withItems(new ArrayList<>(List.of(ItemBuilder.generic(depth - 1).build())));"));
        assertFalse(builder.contains("withDireccion(null)"));
        assertFalse(builder.contains("withItems(null)"));

        String readme = Files.readString(project.resolve("README.md"));
        assertTrue(readme.contains("documentos embebidos"));
        assertTrue(readme.contains("no tienen colección ni API propia"));
    }

    @Test
    void processDomain_withMongodbCompositionToRootEntity_throwsClearError() {
        Domain domain = mongoCompositionDomain();
        domain.getEntities().stream()
                .filter(entity -> entity.getEntityName().equals("direccion"))
                .findFirst()
                .orElseThrow()
                .setEmbedded(false);
        domain.setDatabase(DatabaseEnum.MONGODB);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("documento embebido"));
        assertTrue(exception.getMessage().contains("direccion"));
    }

    @Test
    void processDomain_withMongodbEmbeddedEntityContainingComposition_throwsClearError() {
        Domain domain = mongoCompositionDomain();
        Entity direccion = domain.getEntities().stream()
                .filter(entity -> entity.getEntityName().equals("direccion"))
                .findFirst()
                .orElseThrow();
        direccion.getAttributes().put("item",
                attribute(TypeEnum.COMPOSITION, "item"));
        domain.setDatabase(DatabaseEnum.MONGODB);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("embebida"));
        assertTrue(exception.getMessage().contains("composiciones"));
    }

    @Test
    void processDomain_withCompositionInSql_throwsClearError() {
        Domain domain = mongoCompositionDomain();
        domain.setDatabase(DatabaseEnum.POSTGRESQL);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("solo es válida para MongoDB"));
    }

    @Test
    void processDomain_withEmbeddedEntityInSql_throwsClearError() {
        Domain domain = flatDomain();
        domain.getEntities().get(0).setEmbedded(true);
        domain.setDatabase(DatabaseEnum.POSTGRESQL);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("documento embebido"));
        assertTrue(exception.getMessage().contains("MongoDB"));
    }

    @Test
    void processDomain_withDefaultApi_generatesRestControllers() throws IOException {
        Path project = track(domainService.processDomain(validDomain()));

        assertTrue(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/controller/PersonaController.java")));
        assertFalse(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/controller/PersonaGraphQLController.java")));
        assertFalse(Files.exists(project.resolve("src/main/resources/graphql/schema.graphqls")));
        assertTrue(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/controller/advice/AdviceController.java")));

        String pom = Files.readString(project.resolve("pom.xml"));
        assertFalse(pom.contains("spring-boot-starter-graphql"));
        assertFalse(pom.contains("spring-data-rest-hal-explorer"));
        assertTrue(pom.contains("<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>"));
        assertTrue(pom.contains("<version>3.2.0</version>"));
        assertFalse(pom.contains("API_DEPENDENCY"));
        assertFalse(pom.contains("API_DOCS_DEPENDENCY"));

        String openApiConfig = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/config/OpenApiConfig.java"));
        assertTrue(openApiConfig.contains(".title(\"Mi Proyecto API\")"));

        String personaController = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/controller/PersonaController.java"));
        assertTrue(personaController.contains("@Tag(name = \"Persona\""));
        assertTrue(personaController.contains("@Operation(summary = \"Listar todas las entidades Persona\")"));
        assertTrue(personaController.contains("@Parameter(description = \"ID de Persona\")"));
        assertTrue(personaController.contains("@RequestBody PersonaRequest request"));
        assertTrue(personaController.contains("PersonaMapper.toEntity(request)"));

        String personaRequest = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/dto/PersonaRequest.java"));
        assertTrue(personaRequest.contains("private String nombre;"));
        assertTrue(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/mapper/PersonaMapper.java")));

        String properties = Files.readString(project.resolve("src/main/resources/application.properties"));
        assertFalse(properties.contains("spring.graphql"));
        assertTrue(properties.contains("springdoc.swagger-ui.path=/swagger-ui.html"));
        assertTrue(properties.contains("springdoc.api-docs.path=/v3/api-docs"));
        assertFalse(properties.contains("app.cache.enabled"));
        assertFalse(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/config/CacheConfig.java")));

        String service = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/service/PersonaService.java"));
        assertFalse(service.contains("@Cacheable"));
        assertFalse(service.contains("@CachePut"));
        assertFalse(service.contains("@CacheEvict"));

        String compose = Files.readString(project.resolve("docker-compose.yml"));
        assertFalse(compose.contains("redis"));

        String readme = Files.readString(project.resolve("README.md"));
        assertTrue(readme.contains("## API REST"));
        assertTrue(readme.contains("http://localhost:8080/swagger-ui.html"));
        assertTrue(readme.contains("http://localhost:8080/v3/api-docs"));
        assertFalse(readme.contains("API GraphQL"));
        assertFalse(readme.contains("## Cache Redis"));
    }

    @Test
    void processDomain_withRedisCache_generatesCacheConfigurationAndAnnotations() throws IOException {
        Domain domain = validDomain();
        domain.setDatabase(DatabaseEnum.POSTGRESQL);
        domain.getCache().setEnabled(true);
        domain.getCache().setPrefix("miapp");
        domain.getCache().setTtlSeconds(120);
        domain.getCache().setCacheFindAll(true);

        Path project = track(domainService.processDomain(domain));

        String pom = Files.readString(project.resolve("pom.xml"));
        assertTrue(pom.contains("<artifactId>spring-boot-starter-data-redis</artifactId>"));
        assertTrue(pom.contains("<artifactId>spring-boot-starter-cache</artifactId>"));
        assertFalse(pom.contains("CACHE_DEPENDENCY"));
        assertFalse(pom.contains("CACHE_PROPERTIES"));

        assertTrue(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/config/CacheConfig.java")));
        assertTrue(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/config/LoggingCacheErrorHandler.java")));

        String controller = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/controller/PersonaController.java"));
        assertFalse(controller.contains("CACHE_"));
        assertTrue(controller.contains("import org.springframework.cache.annotation.Cacheable;"));
        assertTrue(controller.contains("@Cacheable(cacheNames = \"persona\", key = \"#id\")"));
        assertTrue(controller.contains("@Cacheable(cacheNames = \"persona-list\", key = \"'all'\")"));
        assertTrue(controller.contains("@Caching(put = @CachePut(cacheNames = \"persona\", key = \"#result.id\"),"));
        assertTrue(controller.contains("evict = @CacheEvict(cacheNames = \"persona-list\", allEntries = true))"));
        assertTrue(controller.contains("@Caching(evict = {@CacheEvict(cacheNames = \"persona\", key = \"#id\"),"));
        assertTrue(controller.contains("@CacheEvict(cacheNames = \"persona-list\", allEntries = true)})"));

        String service = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/service/PersonaService.java"));
        assertFalse(service.contains("@Cacheable"));
        assertFalse(service.contains("@CachePut"));
        assertFalse(service.contains("@CacheEvict"));

        String properties = Files.readString(project.resolve("src/main/resources/application.properties"));
        assertTrue(properties.contains("app.cache.enabled=true"));
        assertTrue(properties.contains("app.cache.prefix=miapp"));
        assertTrue(properties.contains("app.cache.ttl-seconds=120"));
        assertTrue(properties.contains("spring.cache.type=redis"));
        assertTrue(properties.contains("${REDIS_URL:redis://localhost:6379}"));

        String testProperties = Files.readString(project.resolve("src/test/resources/application.properties"));
        assertTrue(testProperties.contains("app.cache.enabled=false"));
        assertTrue(testProperties.contains("spring.cache.type=none"));

        String compose = Files.readString(project.resolve("docker-compose.yml"));
        assertTrue(compose.contains("redis:7-alpine"));
        assertTrue(compose.contains("redis_data:/data"));
        assertTrue(compose.contains("redis_data:"));

        String readme = Files.readString(project.resolve("README.md"));
        assertTrue(readme.contains("## Cache Redis"));
        assertTrue(readme.contains("miapp"));
        assertTrue(readme.contains("120 segundos"));
    }

    @Test
    void processDomain_withRedisCacheAndH2_generatesRedisOnlyCompose() throws IOException {
        Domain domain = validDomain();
        domain.setDatabase(DatabaseEnum.H2);
        domain.getCache().setEnabled(true);

        Path project = track(domainService.processDomain(domain));

        String compose = Files.readString(project.resolve("docker-compose.yml"));
        assertTrue(compose.contains("redis:7-alpine"));
        assertFalse(compose.contains("h2"));
        assertFalse(compose.contains("postgres"));
    }

    @Test
    void processDomain_withInvalidRedisPrefix_throwsClearError() {
        Domain domain = validDomain();
        domain.getCache().setEnabled(true);
        domain.getCache().setPrefix("prefijo con espacios");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("prefijo"));
        assertTrue(exception.getMessage().contains("Redis"));
    }

    @Test
    void processDomain_withCacheEnabledAndEntityOptedOut_onlyAnnotatesSelectedEntities() throws IOException {
        Domain domain = flatDomain();
        domain.getCache().setEnabled(true);
        domain.getEntities().get(0).setCacheEnabled(false);

        Path project = track(domainService.processDomain(domain));

        String personaController = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/controller/PersonaController.java"));
        assertFalse(personaController.contains("@Cacheable"));
        assertFalse(personaController.contains("@CachePut"));
        assertFalse(personaController.contains("@CacheEvict"));

        String pedidoController = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/controller/PedidoController.java"));
        assertTrue(pedidoController.contains("@Cacheable(cacheNames = \"pedido\", key = \"#id\")"));
        assertTrue(pedidoController.contains("@CachePut(cacheNames = \"pedido\", key = \"#result.id\")"));

        String readme = Files.readString(project.resolve("README.md"));
        assertTrue(readme.contains("Entidades cacheadas: `pedido`"));
        assertFalse(readme.contains("Entidades cacheadas: `persona`"));
    }

    @Test
    void processDomain_withCacheEnabledAndNoCachedEntities_throwsClearError() {
        Domain domain = flatDomain();
        domain.getCache().setEnabled(true);
        domain.getEntities().forEach(entity -> entity.setCacheEnabled(false));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("al menos una entidad"));
    }

    @Test
    void processDomain_withCacheEnabledAndEmbeddedEntitiesOnly_throwsClearError() {
        Domain domain = mongoCompositionDomain();
        domain.setDatabase(DatabaseEnum.MONGODB);
        domain.getCache().setEnabled(true);
        domain.getEntities().stream()
                .filter(entity -> !entity.isEmbedded())
                .forEach(entity -> entity.setCacheEnabled(false));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("al menos una entidad"));
    }

    @Test
    void processDomain_withGraphql_generatesGraphqlControllersSchemaAndConfiguration() throws IOException {
        Domain domain = validDomain();
        domain.setApiType(ApiEnum.GRAPHQL);

        Path project = track(domainService.processDomain(domain));

        String pom = Files.readString(project.resolve("pom.xml"));
        assertTrue(pom.contains("<artifactId>spring-boot-starter-graphql</artifactId>"));
        assertFalse(pom.contains("spring-data-rest-hal-explorer"));
        assertFalse(pom.contains("springdoc-openapi-starter-webmvc-ui"));
        assertFalse(pom.contains("API_DEPENDENCY"));
        assertFalse(pom.contains("API_DOCS_DEPENDENCY"));
        assertFalse(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/config/OpenApiConfig.java")));

        String personaController = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/controller/PersonaGraphQLController.java"));
        assertTrue(personaController.contains("@QueryMapping"));
        assertTrue(personaController.contains("findAllPersona()"));
        assertTrue(personaController.contains("findPersonaById(@Argument(\"id\") String id)"));
        assertTrue(personaController.contains("@MutationMapping"));
        assertTrue(personaController.contains("createPersona(@Argument(\"input\") PersonaRequest input)"));
        assertTrue(personaController.contains("input.setId(null);"));
        assertTrue(personaController.contains("personaService.save(PersonaMapper.toEntity(input))"));
        assertTrue(personaController.contains("personaService.findById(Long.valueOf(id))"));
        assertFalse(personaController.contains("@SchemaMapping"));

        String pedidoController = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/controller/PedidoGraphQLController.java"));
        assertTrue(pedidoController.contains("pedidoService.delete(Long.valueOf(id))"));

        assertFalse(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/controller/PersonaController.java")));
        assertFalse(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/controller/advice/AdviceController.java")));
        assertTrue(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/controller/advice/GraphQLExceptionHandler.java")));

        assertTrue(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/config/GraphQLScalarConfig.java")));

        String scalarConfig = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/config/GraphQLScalarConfig.java"));
        assertTrue(scalarConfig.contains("RuntimeWiringConfigurer"));
        assertTrue(scalarConfig.contains("\"Date\""));
        assertTrue(scalarConfig.contains("\"DateTime\""));
        assertTrue(scalarConfig.contains("\"Time\""));
        assertTrue(scalarConfig.contains("\"BigDecimal\""));
        assertTrue(scalarConfig.contains("\"UUID\""));

        String schema = Files.readString(project.resolve("src/main/resources/graphql/schema.graphqls"));
        assertTrue(schema.contains("scalar Date\nscalar DateTime\nscalar Time\nscalar BigDecimal\nscalar UUID\n"));
        assertTrue(schema.contains("type Persona {\n  id: ID!\n  fechaCreacion: DateTime\n"));
        assertTrue(schema.contains("input PersonaInput {\n  id: ID\n"));
        assertTrue(schema.contains("type Pedido {"));
        assertTrue(schema.contains("cliente: Persona"));
        assertTrue(schema.contains("items: [Persona]"));
        assertTrue(schema.contains("input PedidoInput {"));
        assertTrue(schema.contains("clienteId: ID"));
        assertTrue(schema.contains("itemsIds: [ID]"));
        assertTrue(schema.contains("findAllPersona: [Persona!]!"));
        assertTrue(schema.contains("findPersonaById(id: ID!): Persona"));
        assertTrue(schema.contains("createPedido(input: PedidoInput!): Pedido!"));
        assertTrue(schema.contains("updatePedido(input: PedidoInput!): Pedido!"));
        assertTrue(schema.contains("deletePedido(id: ID!): Boolean!"));

        String properties = Files.readString(project.resolve("src/main/resources/application.properties"));
        assertTrue(properties.contains("spring.graphql.graphiql.enabled=true"));
        assertTrue(properties.contains("spring.graphql.schema.printer.enabled=true"));
        assertFalse(properties.contains("springdoc"));

        String readme = Files.readString(project.resolve("README.md"));
        assertTrue(readme.contains("## API GraphQL"));
        assertTrue(readme.contains("http://localhost:8080/graphiql"));
        assertTrue(readme.contains("La API GraphQL queda disponible en `http://localhost:8080/graphql`."));
    }

    @Test
    void processDomain_withMongodbAndGraphql_generatesCompositionInputsAndStringIds() throws IOException {
        Domain domain = mongoCompositionDomain();
        domain.setDatabase(DatabaseEnum.MONGODB);
        domain.setApiType(ApiEnum.GRAPHQL);

        Path project = track(domainService.processDomain(domain));

        String controller = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/controller/PedidoGraphQLController.java"));
        assertTrue(controller.contains("pedidoService.findById(id)"));
        assertTrue(controller.contains("pedidoService.delete(id)"));

        assertTrue(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/controller/PedidoGraphQLController.java")));
        assertFalse(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/controller/DireccionGraphQLController.java")));
        assertFalse(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/controller/ItemGraphQLController.java")));

        String schema = Files.readString(project.resolve("src/main/resources/graphql/schema.graphqls"));
        assertTrue(schema.contains("type Pedido {"));
        assertTrue(schema.contains("direccion: Direccion"));
        assertTrue(schema.contains("items: [Item]"));
        assertTrue(schema.contains("input DireccionInput {\n  calle: String\n}"));
        assertTrue(schema.contains("input ItemInput {\n  producto: String\n}"));
        assertTrue(schema.contains("direccion: DireccionInput"));
        assertTrue(schema.contains("items: [ItemInput]"));
        assertTrue(schema.contains("type Query {\n  findAllPedido: [Pedido!]!\n"));
        assertFalse(schema.contains("findAllDireccion"));

        String readme = Files.readString(project.resolve("README.md"));
        assertTrue(readme.contains("## API GraphQL"));
    }

    @Test
    void processDomain_withGraphqlAndReservedTypeName_throwsClearError() {
        Domain domain = validDomain();
        domain.setApiType(ApiEnum.GRAPHQL);
        domain.getEntities().add(entity("query"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("reservado"));
    }

    @Test
    void processDomain_withGraphqlAndDuplicatedField_throwsClearError() {
        Domain domain = validDomain();
        domain.setApiType(ApiEnum.GRAPHQL);
        domain.getEntities().get(1).getAttributes().put("items", attribute(TypeEnum.STRING, null));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> domainService.processDomain(domain));

        assertTrue(exception.getMessage().contains("campo duplicado"));
    }

    @Test
    void processDomain_withRelations_generatesSafeCascadesAndResponseDto() throws IOException {
        Path project = track(domainService.processDomain(validDomain()));

        String pedido = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/domain/Pedido.java"));
        assertTrue(pedido.contains("@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})"));
        assertTrue(pedido.contains("@OneToMany(cascade = CascadeType.ALL)"));
        assertTrue(pedido.contains("@ToString.Exclude"));
        assertTrue(pedido.contains("@EqualsAndHashCode.Exclude"));

        assertTrue(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/dto/PedidoResponse.java")));
        String response = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/dto/PedidoResponse.java"));
        assertTrue(response.contains("private Long clienteId;"));
        assertTrue(response.contains("private List<Long> itemsIds;"));

        String responseMapper = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/mapper/PedidoResponseMapper.java"));
        assertTrue(responseMapper.contains(
                "response.setClienteId(entity.getCliente() != null ? entity.getCliente().getId() : null);"));
        assertTrue(responseMapper.contains(
                "response.setItemsIds(entity.getItems().stream().map(Persona::getId).toList());"));

        String controller = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/controller/PedidoController.java"));
        assertTrue(controller.contains("public List<PedidoResponse> findAll()"));
        assertTrue(controller.contains("public PedidoResponse findById("));
        assertTrue(controller.contains("@ResponseStatus(HttpStatus.CREATED)"));
        assertTrue(controller.contains("PedidoResponseMapper.toResponse"));
        assertTrue(controller.contains("PedidoMapper.toEntity(request)"));
    }

    @Test
    void processDomain_withNotFoundAndEditGuard_generatesNotFoundException() throws IOException {
        Path project = track(domainService.processDomain(validDomain()));

        assertTrue(Files.exists(project.resolve(
                "src/main/java/com/example/projectBase/controller/advice/NotFoundException.java")));

        String personaService = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/service/PersonaService.java"));
        assertTrue(personaService.contains("throw new NotFoundException"));
        assertTrue(personaService.contains("personaRepository.findById(persona.getId())"));
        assertTrue(personaService.contains("persona.setFechaCreacion(existing.getFechaCreacion());"));

        String advice = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/controller/advice/AdviceController.java"));
        assertTrue(advice.contains("handleNotFound"));
        assertTrue(advice.contains("HttpStatus.NOT_FOUND"));
        assertTrue(advice.contains("handleConflict"));
        assertTrue(advice.contains("DataIntegrityViolationException"));
    }

    @Test
    void processDomain_withGraphql_generatesNotFoundHandler() throws IOException {
        Domain domain = validDomain();
        domain.setApiType(ApiEnum.GRAPHQL);

        Path project = track(domainService.processDomain(domain));

        String handler = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/controller/advice/GraphQLExceptionHandler.java"));
        assertTrue(handler.contains("ErrorType.NOT_FOUND"));
        assertTrue(handler.contains("handleNotFound"));
        assertTrue(handler.contains("DataIntegrityViolationException"));
    }

    @Test
    void processDomain_withMongodbCreate_clearsIdAndCreationDate() throws IOException {
        Domain domain = flatDomain();
        domain.setDatabase(DatabaseEnum.MONGODB);

        Path project = track(domainService.processDomain(domain));

        String controller = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/controller/PersonaController.java"));
        assertTrue(controller.contains("request.setId(null);"));
        assertTrue(controller.contains("request.setFechaCreacion(null);"));
        assertTrue(controller.contains("ResponseEntity.status(HttpStatus.CREATED)"));

        String service = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/service/PersonaService.java"));
        assertTrue(service.contains("throw new NotFoundException"));
        assertTrue(service.contains("personaRepository.findById(persona.getId())"));
        assertTrue(service.contains("persona.setFechaCreacion(existing.getFechaCreacion());"));
    }

    @Test
    void processDomain_withMongodbCompositionAndEmbeddedValidations_generatesValidateCalls() throws IOException {
        Domain domain = mongoCompositionDomain();
        domain.setDatabase(DatabaseEnum.MONGODB);
        Entity pedido = domain.getEntities().get(0);
        pedido.getAttributes().put("numero",
                attribute(TypeEnum.STRING, null, ValidationEnum.NOT_BLANK));
        Entity direccion = domain.getEntities().get(1);
        direccion.getAttributes().put("calle",
                attribute(TypeEnum.STRING, null, ValidationEnum.NOT_BLANK));
        Entity item = domain.getEntities().get(2);
        item.getAttributes().put("producto",
                attribute(TypeEnum.STRING, null, ValidationEnum.NOT_BLANK));

        Path project = track(domainService.processDomain(domain));

        String direccionFile = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/domain/Direccion.java"));
        assertTrue(direccionFile.contains("public void validate()"));
        assertTrue(direccionFile.contains(
                "Assert.isTrue(getCalle() != null && !getCalle().isBlank(), \"calle can not be blank\");"));

        String itemFile = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/domain/Item.java"));
        assertTrue(itemFile.contains(
                "Assert.isTrue(getProducto() != null && !getProducto().isBlank(), \"producto can not be blank\");"));

        String service = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/service/PedidoService.java"));
        assertTrue(service.contains("import com.example.projectBase.domain.Direccion;"));
        assertTrue(service.contains("import com.example.projectBase.domain.Item;"));
        assertTrue(service.contains("pedido.getDireccion().validate();"));
        assertTrue(service.contains("itemItem.validate();"));
    }

    @Test
    void processDomain_withBidirectionalList_generatesMappedByAndSetsBackReference() throws IOException {
        Domain domain = new Domain();
        domain.setTitle("Mi Proyecto");
        domain.setDatabase(DatabaseEnum.POSTGRESQL);

        Entity pedido = entity("pedido");
        pedido.getAttributes().put("items", attribute(TypeEnum.CUSTOM_LIST, "item"));
        Entity item = entity("item");
        item.getAttributes().put("pedido", attribute(TypeEnum.CUSTOM, "pedido"));
        domain.setEntities(new ArrayList<>(List.of(pedido, item)));

        Path project = track(domainService.processDomain(domain));

        String pedidoFile = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/domain/Pedido.java"));
        assertTrue(pedidoFile.contains("@OneToMany(mappedBy = \"pedido\", cascade = CascadeType.ALL)"));
        assertFalse(pedidoFile.contains("@JoinColumn(name = \"pedido_items_id\")"));

        String service = Files.readString(project.resolve(
                "src/main/java/com/example/projectBase/service/PedidoService.java"));
        assertTrue(service.contains("itemValue.setPedido(pedido);"));
        assertTrue(service.contains(
                ".orElseThrow(() -> new IllegalArgumentException(\"Item not found with id: \" + itemValue.getId()));"));

        String schema = Files.readString(project.resolve("doc/scripts/schema.sql"));
        assertTrue(schema.contains("\"pedido_id\" BIGINT"));
        assertFalse(schema.contains("pedido_items_id"));
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

    private Domain flatDomain() {
        Domain domain = new Domain();
        domain.setTitle("Mi Proyecto");

        Entity persona = entity("persona");
        persona.getAttributes().put("nombre",
                attribute(TypeEnum.STRING, null, ValidationEnum.NOT_BLANK));

        Entity pedido = entity("pedido");
        pedido.getAttributes().put("total", attribute(TypeEnum.INTEGER, null));

        domain.setEntities(new ArrayList<>(List.of(persona, pedido)));
        return domain;
    }

    private Domain mongoCompositionDomain() {
        Domain domain = new Domain();
        domain.setTitle("Mi Proyecto");

        Entity pedido = entity("pedido");
        pedido.getAttributes().put("numero", attribute(TypeEnum.STRING, null));
        pedido.getAttributes().put("direccion",
                attribute(TypeEnum.COMPOSITION, "direccion"));
        pedido.getAttributes().put("item",
                attribute(TypeEnum.COMPOSITION_LIST, "item"));

        Entity direccion = entity("direccion");
        direccion.setEmbedded(true);
        direccion.getAttributes().put("calle", attribute(TypeEnum.STRING, null));

        Entity item = entity("item");
        item.setEmbedded(true);
        item.getAttributes().put("producto", attribute(TypeEnum.STRING, null));

        domain.setEntities(new ArrayList<>(List.of(pedido, direccion, item)));
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
