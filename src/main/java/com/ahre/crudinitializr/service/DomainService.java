package com.ahre.crudinitializr.service;

import com.ahre.crudinitializr.enums.ApiEnum;
import com.ahre.crudinitializr.enums.DatabaseEnum;
import com.ahre.crudinitializr.enums.TypeEnum;
import com.ahre.crudinitializr.enums.ValidationEnum;
import com.ahre.crudinitializr.utils.FileUtils;
import com.ahre.crudinitializr.utils.StringUtils;
import com.ahre.crudinitializr.vo.Attribute;
import com.ahre.crudinitializr.vo.Domain;
import com.ahre.crudinitializr.vo.Entity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DomainService {

    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null", "_"
    );

    private final FileUtils fileUtils;
    private final String srcPath;
    private final String testPath;
    private final String importProjectBasePath;

    public DomainService(FileUtils fileUtils,
                         @Value("${srcPath}") String srcPath,
                         @Value("${testPath}") String testPath,
                         @Value("${importProjectBasePath}") String importProjectBasePath) {
        this.fileUtils = fileUtils;
        this.srcPath = srcPath;
        this.testPath = testPath;
        this.importProjectBasePath = importProjectBasePath;
    }

    public Path processDomain(Domain domain) {
        validateDomain(domain);
        DatabaseEnum database = domain.getDatabase() == null ? DatabaseEnum.POSTGRESQL : domain.getDatabase();
        ApiEnum apiType = domain.getApiType() == null ? ApiEnum.REST : domain.getApiType();
        validateDatabaseCompatibility(domain, database);
        validateApiCompatibility(domain, apiType);

        Path project = null;
        try {
            project = fileUtils.createOutputFolder();
            writeStaticFiles(project, database, apiType);
            writeOpenApiConfig(domain.getTitle(), apiType, project);
            writeProjectReadme(domain.getTitle(), database, apiType, project);
            writeDockerCompose(domain.getTitle(), database, project);
            writePomFile(domain.getTitle(), database, apiType, project);
            writeApplicationProperties(database, apiType, project);
            for (Entity entity : domain.getEntities()) {
                writeDomainFile(entity, database, project);
                writeServiceFile(entity, database, project);
                writeRepositoryFile(entity, database, project);
                if (apiType.isGraphQL()) {
                    writeGraphQlControllerFile(entity, database, project);
                } else {
                    writeControllerFile(entity, database, project);
                }
                writeBuilderFile(entity, project);
                writeTestFile(entity, database, project);
            }
            if (apiType.isGraphQL()) {
                writeGraphQlSchema(domain, project);
            }
            writeSchemaSQLFiles(domain, database, project);
            return project;
        } catch (IOException e) {
            fileUtils.deleteQuietly(project);
            throw new IllegalArgumentException("No se pudo crear el proyecto", e);
        } catch (RuntimeException e) {
            fileUtils.deleteQuietly(project);
            throw e;
        }
    }

    void validateDomain(Domain domain) {
        Assert.notNull(domain, "El dominio no puede estar vacío");

        String title = domain.getTitle();
        Assert.notNull(title, "El nombre del proyecto no puede estar vacío");
        Assert.isTrue(!title.trim().isEmpty(), "El nombre del proyecto no puede estar vacío");
        Assert.isTrue(title.trim().length() <= 30, "El nombre del proyecto no puede tener más de 30 caracteres");

        List<Entity> entities = domain.getEntities();
        Assert.notNull(entities, "La lista de entidades no puede ser nula");
        Assert.notEmpty(entities, "El proyecto debe tener al menos una entidad");

        Set<String> entityClassNames = new HashSet<>();
        Set<String> tableNames = new HashSet<>();
        for (Entity entity : entities) {
            Assert.notNull(entity, "Las entidades no pueden ser nulas");

            String entityName = entity.getEntityName();
            Assert.isTrue(entityName != null && !entityName.trim().isEmpty(), "El nombre de una entidad no puede estar vacío");
            Assert.isTrue(entityName.trim().length() <= 30, "El nombre de la entidad '" + entityName + "' no puede tener más de 30 caracteres");
            Assert.isTrue(isJavaIdentifier(entityName), "El nombre de la entidad '" + entityName + "' no es válido: debe empezar con una letra y no contener espacios ni caracteres especiales");

            String entityClassName = StringUtils.capitalizeFirstCharacter(entityName);
            Assert.isTrue(entityClassNames.add(entityClassName), "El nombre de la entidad '" + entityName + "' está repetido");

            String tableName = StringUtils.transformToSnakeCase(entityName);
            Assert.isTrue(tableNames.add(tableName), "El nombre de la entidad '" + entityName + "' genera la tabla repetida '" + tableName + "'");

            Map<String, Attribute> attributes = entity.getAttributes();
            Assert.notNull(attributes, "La lista de atributos de la entidad '" + entityName + "' no puede ser nula");

            Set<String> attributeNames = new HashSet<>();
            Set<String> attributeColumns = new HashSet<>();
            for (Map.Entry<String, Attribute> entry : attributes.entrySet()) {
                String key = entry.getKey();
                Attribute value = entry.getValue();

                Assert.isTrue(key != null && !key.trim().isEmpty(), "Todos los atributos deben tener un nombre");
                Assert.isTrue(key.trim().length() <= 30, "El nombre del atributo '" + key + "' no puede tener más de 30 caracteres");
                Assert.isTrue(isJavaIdentifier(key), "El nombre del atributo '" + key + "' no es válido: debe empezar con una letra y no contener espacios ni caracteres especiales");
                Assert.isTrue(attributeNames.add(StringUtils.capitalizeFirstCharacter(key)), "El atributo '" + key + "' está repetido en la entidad '" + entityName + "'");

                String columnName = StringUtils.transformToSnakeCase(key);
                Assert.isTrue(!columnName.equals("id") && !columnName.equals("fecha_creacion"),
                        "El atributo '" + key + "' no puede llamarse id ni fechaCreacion: son columnas reservadas de la entidad");
                Assert.isTrue(attributeColumns.add(columnName),
                        "El atributo '" + key + "' genera la columna duplicada '" + columnName + "' en la entidad '" + entityName + "'");

                Assert.notNull(value, "Todos los atributos deben tener un tipo");
                TypeEnum type = value.getType();
                Assert.notNull(type, "Todos los atributos deben tener un tipo");

                if (isLink(type)) {
                    String customType = value.getCustomType();
                    Assert.isTrue(customType != null && !customType.trim().isEmpty(), "El tipo de relación del atributo '" + key + "' no puede estar vacío");
                    Assert.isTrue(isJavaIdentifier(customType), "El tipo de relación '" + customType + "' del atributo '" + key + "' no es válido");
                }

                List<ValidationEnum> validations = value.getValidations();
                boolean hasMinValidation = false;
                boolean hasMaxValidation = false;
                if (validations != null) {
                    for (ValidationEnum validation : validations) {
                        if (validation == null) {
                            continue;
                        }
                        boolean allowed = switch (validation) {
                            case NOT_NULL -> true;
                            case NOT_BLANK -> type == TypeEnum.STRING;
                            case NOT_EMPTY -> type == TypeEnum.STRING || isList(type);
                            case PAST, PAST_OR_PRESENT, FUTURE, FUTURE_OR_PRESENT -> isTemporal(type);
                            case POSITIVE, NEGATIVE, POSITIVE_OR_ZERO, NEGATIVE_OR_ZERO, MIN, MAX -> isNumeric(type);
                        };
                        Assert.isTrue(allowed, "La validación " + validation + " no es válida para el atributo '" + key + "' de tipo " + type);
                        hasMinValidation = hasMinValidation || validation == ValidationEnum.MIN;
                        hasMaxValidation = hasMaxValidation || validation == ValidationEnum.MAX;
                    }
                }

                if (hasMinValidation) {
                    Assert.notNull(value.getMinValue(), "El atributo '" + key + "' requiere un valor mínimo");
                }
                if (hasMaxValidation) {
                    Assert.notNull(value.getMaxValue(), "El atributo '" + key + "' requiere un valor máximo");
                }
                if (value.getMinValue() != null && value.getMaxValue() != null) {
                    Assert.isTrue(value.getMinValue().compareTo(value.getMaxValue()) <= 0,
                            "El valor mínimo del atributo '" + key + "' no puede ser mayor que el máximo");
                }
                validateNumericBound(type, value.getMinValue(), key, "mínimo");
                validateNumericBound(type, value.getMaxValue(), key, "máximo");
                validateNumericValidationConsistency(value, key);
            }
        }

        for (Entity entity : entities) {
            String entityClassName = StringUtils.capitalizeFirstCharacter(entity.getEntityName());
            for (Map.Entry<String, Attribute> entry : entity.getAttributes().entrySet()) {
                Attribute value = entry.getValue();
                if (isLink(value.getType())) {
                    String customClassName = StringUtils.capitalizeFirstCharacter(value.getCustomType());
                    Assert.isTrue(entityClassNames.contains(customClassName),
                            "El atributo '" + entry.getKey() + "' de la entidad '" + entity.getEntityName() + "' referencia a una entidad inexistente: " + value.getCustomType());
                    Assert.isTrue(!customClassName.equals(entityClassName),
                            "El atributo '" + entry.getKey() + "' de la entidad '" + entity.getEntityName() + "' no puede referenciarse a sí mismo");
                }
            }
        }
    }

    private void validateDatabaseCompatibility(Domain domain, DatabaseEnum database) {
        if (database.isDocument()) {
            validateMongoCompatibility(domain);
        } else {
            validateSqlCompatibility(domain);
        }
    }

    private void validateApiCompatibility(Domain domain, ApiEnum apiType) {
        if (!apiType.isGraphQL()) {
            return;
        }

        Set<String> reservedTypeNames = Set.of(
                "String", "Int", "Float", "Boolean", "ID", "Query", "Mutation", "Subscription",
                "Date", "DateTime", "Time", "BigDecimal", "UUID");
        Set<String> schemaTypeNames = new HashSet<>();
        boolean hasRootEntity = false;

        for (Entity entity : domain.getEntities()) {
            if (!entity.isEmbedded()) {
                hasRootEntity = true;
            }

            String entityClassName = StringUtils.capitalizeFirstCharacter(entity.getEntityName());
            Assert.isTrue(!reservedTypeNames.contains(entityClassName),
                    "La entidad '" + entity.getEntityName() + "' no puede llamarse '" + entityClassName
                            + "' cuando se genera una API GraphQL: es un tipo reservado del esquema");
            Assert.isTrue(schemaTypeNames.add(entityClassName),
                    "El esquema GraphQL generaría el tipo duplicado '" + entityClassName + "'");
            Assert.isTrue(schemaTypeNames.add(entityClassName + "Input"),
                    "El esquema GraphQL generaría el tipo duplicado '" + entityClassName
                            + "Input'. Renombrá la entidad que entra en conflicto");

            Set<String> outputFields = new HashSet<>();
            Set<String> inputFields = new HashSet<>();
            if (!entity.isEmbedded()) {
                outputFields.add("id");
                outputFields.add("fechaCreacion");
                inputFields.add("id");
            }

            for (Map.Entry<String, Attribute> entry : entity.getAttributes().entrySet()) {
                String fieldName = graphQlFieldName(entry.getKey(), entry.getValue().getType());
                Assert.isTrue(outputFields.add(fieldName),
                        "El esquema GraphQL generaría el campo duplicado '" + fieldName
                                + "' en el tipo '" + entityClassName + "'");
                Assert.isTrue(inputFields.add(fieldName),
                        "El esquema GraphQL generaría el campo duplicado '" + fieldName
                                + "' en el input '" + entityClassName + "Input'");
            }
        }

        Assert.isTrue(hasRootEntity,
                "El proyecto debe tener al menos una entidad raíz para generar una API GraphQL");
    }

    private void validateSqlCompatibility(Domain domain) {
        for (Entity entity : domain.getEntities()) {
            if (entity.isEmbedded()) {
                throw new IllegalArgumentException(
                        "La entidad '" + entity.getEntityName()
                                + "' está marcada como documento embebido y solo es válida para MongoDB.");
            }
            for (Map.Entry<String, Attribute> entry : entity.getAttributes().entrySet()) {
                if (isComposition(entry.getValue().getType())) {
                    throw new IllegalArgumentException(
                            "La composición '" + entry.getKey() + "' de la entidad '" + entity.getEntityName()
                                    + "' solo es válida para MongoDB. Cambiala a una relación SQL o eliminála.");
                }
            }
        }
    }

    private void validateMongoCompatibility(Domain domain) {
        Map<String, Entity> entitiesByName = new HashMap<>();
        for (Entity entity : domain.getEntities()) {
            entitiesByName.put(StringUtils.capitalizeFirstCharacter(entity.getEntityName()), entity);
        }

        for (Entity entity : domain.getEntities()) {
            for (Map.Entry<String, Attribute> entry : entity.getAttributes().entrySet()) {
                Attribute attribute = entry.getValue();
                if (isRelation(attribute.getType())) {
                    throw new IllegalArgumentException(
                            "MongoDB no soporta relaciones SQL en esta versión. El atributo '" + entry.getKey()
                                    + "' de la entidad '" + entity.getEntityName()
                                    + "' es una relación. Cambialo a una composición o elegí una base de datos SQL.");
                }

                if (!isComposition(attribute.getType())) {
                    continue;
                }

                if (entity.isEmbedded()) {
                    throw new IllegalArgumentException(
                            "La entidad embebida '" + entity.getEntityName()
                                    + "' no puede contener composiciones en esta versión.");
                }

                Entity target = entitiesByName.get(StringUtils.capitalizeFirstCharacter(attribute.getCustomType()));
                if (target == null || !target.isEmbedded()) {
                    throw new IllegalArgumentException(
                            "La composición '" + entry.getKey() + "' de la entidad '" + entity.getEntityName()
                                    + "' debe apuntar a un documento embebido.");
                }
            }
        }
    }

    private boolean isLink(TypeEnum type) {
        return isRelation(type) || isComposition(type);
    }

    private boolean isRelation(TypeEnum type) {
        return type == TypeEnum.CUSTOM
                || type == TypeEnum.CUSTOM_LIST
                || type == TypeEnum.CUSTOM_ONE_TO_ONE
                || type == TypeEnum.CUSTOM_MANY_TO_MANY;
    }

    private boolean isComposition(TypeEnum type) {
        return type == TypeEnum.COMPOSITION || type == TypeEnum.COMPOSITION_LIST;
    }

    private boolean isList(TypeEnum type) {
        return type == TypeEnum.CUSTOM_LIST
                || type == TypeEnum.CUSTOM_MANY_TO_MANY
                || type == TypeEnum.COMPOSITION_LIST;
    }

    private boolean isNumeric(TypeEnum type) {
        return type == TypeEnum.INTEGER
                || type == TypeEnum.LONG
                || type == TypeEnum.DECIMAL
                || type == TypeEnum.DOUBLE;
    }

    private boolean isTemporal(TypeEnum type) {
        return type == TypeEnum.DATE
                || type == TypeEnum.DATETIME
                || type == TypeEnum.TIME;
    }

    private void validateNumericValidationConsistency(Attribute attribute, String attributeName) {
        List<ValidationEnum> validations = attribute.getValidations();
        if (validations == null || validations.isEmpty()) {
            return;
        }

        boolean positive = validations.contains(ValidationEnum.POSITIVE);
        boolean negative = validations.contains(ValidationEnum.NEGATIVE);
        boolean positiveOrZero = validations.contains(ValidationEnum.POSITIVE_OR_ZERO);
        boolean negativeOrZero = validations.contains(ValidationEnum.NEGATIVE_OR_ZERO);
        BigDecimal minValue = attribute.getMinValue();
        BigDecimal maxValue = attribute.getMaxValue();

        Assert.isTrue(!(positive && (negative || negativeOrZero)),
                "El atributo '" + attributeName + "' no puede ser positivo y negativo a la vez");
        Assert.isTrue(!(negative && positiveOrZero),
                "El atributo '" + attributeName + "' no puede ser negativo y positivo o cero a la vez");
        Assert.isTrue(!(positive && maxValue != null && maxValue.compareTo(BigDecimal.ZERO) <= 0),
                "El atributo '" + attributeName + "' es positivo, pero su valor máximo no lo permite");
        Assert.isTrue(!(positiveOrZero && maxValue != null && maxValue.compareTo(BigDecimal.ZERO) < 0),
                "El atributo '" + attributeName + "' es positivo o cero, pero su valor máximo no lo permite");
        Assert.isTrue(!(negative && minValue != null && minValue.compareTo(BigDecimal.ZERO) >= 0),
                "El atributo '" + attributeName + "' es negativo, pero su valor mínimo no lo permite");
        Assert.isTrue(!(negativeOrZero && minValue != null && minValue.compareTo(BigDecimal.ZERO) > 0),
                "El atributo '" + attributeName + "' es negativo o cero, pero su valor mínimo no lo permite");
    }

    private void validateNumericBound(TypeEnum type, BigDecimal bound, String attributeName, String boundName) {
        if (bound == null || !isNumeric(type)) {
            return;
        }
        if (type == TypeEnum.INTEGER) {
            Assert.isTrue(bound.stripTrailingZeros().scale() <= 0,
                    "El valor " + boundName + " del atributo '" + attributeName + "' debe ser un número entero");
            Assert.isTrue(bound.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) >= 0
                            && bound.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) <= 0,
                    "El valor " + boundName + " del atributo '" + attributeName + "' está fuera del rango de Integer");
        } else if (type == TypeEnum.LONG) {
            Assert.isTrue(bound.stripTrailingZeros().scale() <= 0,
                    "El valor " + boundName + " del atributo '" + attributeName + "' debe ser un número entero");
            Assert.isTrue(bound.compareTo(BigDecimal.valueOf(Long.MIN_VALUE)) >= 0
                            && bound.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) <= 0,
                    "El valor " + boundName + " del atributo '" + attributeName + "' está fuera del rango de Long");
        }
    }

    private void writeStaticFiles(Path project, DatabaseEnum database, ApiEnum apiType) throws IOException {
        if (database.isDocument()) {
            fileUtils.copyTemplate(templatePath(database, srcPath + "/ProjectBaseApplication.java"),
                    project, destinationPath(srcPath + "/ProjectBaseApplication.java"));
            fileUtils.copyTemplate(templatePath(database, testPath + "/tests/ApplicationTests.java"),
                    project, destinationPath(testPath + "/tests/ApplicationTests.java"));
        } else {
            List<String> templates = List.of(
                    srcPath + "/ProjectBaseApplication.java",
                    testPath + "/tests/ApplicationTests.java",
                    "/src/test/resources/application.properties"
            );
            for (String template : templates) {
                fileUtils.copyTemplate(template, project);
            }
        }

        if (apiType.isGraphQL()) {
            fileUtils.copyTemplate(srcPath + "/controller/advice/GraphQLExceptionHandler.java", project);
            fileUtils.copyTemplate(srcPath + "/config/GraphQLScalarConfig.java", project);
        } else {
            fileUtils.copyTemplate(srcPath + "/controller/advice/AdviceController.java", project);
        }
        fileUtils.copyTemplate("/gitignore.template", project, ".gitignore");
    }

    private void writeOpenApiConfig(String title, ApiEnum apiType, Path project) throws IOException {
        if (apiType.isGraphQL()) {
            return;
        }
        String template = fileUtils.getTemplateContent(srcPath + "/config/OpenApiConfig.java");
        String content = template.replace("PROJECT_NAME", escapeJavaString(title.trim()));
        fileUtils.writeFile(project + srcPath + "/config/OpenApiConfig.java", content);
    }

    private String templatePath(DatabaseEnum database, String path) {
        return database.isDocument() ? "/mongo" + path : path;
    }

    private String destinationPath(String path) {
        return path.replaceFirst("^/+", "");
    }

    private void writePomFile(String title, DatabaseEnum database, ApiEnum apiType, Path project) throws IOException {
        String content = fileUtils.getTemplateContent("/pom.xml");
        String escapedTitle = StringUtils.escapeXml(title.trim());
        content = content.replace("PROJECT_ARTIFACT_ID", StringUtils.toArtifactId(title));
        content = content.replace("PROJECT_NAME", escapedTitle);
        content = content.replace("PROJECT_DESCRIPTION", escapedTitle);
        content = content.replace("DATABASE_LABEL", database.getLabel());
        content = content.replace("PERSISTENCE_DEPENDENCY", renderPersistenceDependency(database));
        content = content.replace("API_DEPENDENCY", apiType.isGraphQL()
                ? renderDependency("org.springframework.boot", "spring-boot-starter-graphql", null)
                : "");
        content = content.replace("API_DOCS_DEPENDENCY", apiType.isGraphQL()
                ? ""
                : renderDependency("org.springdoc", "springdoc-openapi-starter-webmvc-ui", "3.2.0", null));
        if (database.isDocument()) {
            content = content.replace("DATABASE_DEPENDENCY", "");
            content = content.replace("TEST_SUPPORT_DEPENDENCY", renderMongoTestDependencies());
        } else {
            content = content.replace("DATABASE_DEPENDENCY", renderDatabaseDependencies(database));
            content = content.replace("TEST_SUPPORT_DEPENDENCY",
                    database.isEmbedded() ? "" : renderDependency("com.h2database", "h2", "test"));
        }
        fileUtils.writeFile(project + "/pom.xml", content);
    }

    private String renderPersistenceDependency(DatabaseEnum database) {
        if (database.isDocument()) {
            return renderDependency("org.springframework.boot", "spring-boot-starter-data-mongodb", null);
        }
        return renderDependency("org.springframework.boot", "spring-boot-starter-data-jpa", null);
    }

    private String renderDatabaseDependencies(DatabaseEnum database) {
        String dependencies = renderDependency(
                database.getDriverGroupId(), database.getDriverArtifactId(), "runtime");
        if (database == DatabaseEnum.SQLITE) {
            dependencies += "\n" + renderDependency(
                    "org.hibernate.orm", "hibernate-community-dialects", "runtime");
        }
        return dependencies;
    }

    private String renderMongoTestDependencies() {
        return renderDependency("org.springframework.boot", "spring-boot-testcontainers", "test")
                + "\n" + renderDependency("org.testcontainers", "testcontainers-mongodb", "test")
                + "\n" + renderDependency("org.testcontainers", "testcontainers-junit-jupiter", "test");
    }

    private String renderDependency(String groupId, String artifactId, String scope) {
        return renderDependency(groupId, artifactId, null, scope);
    }

    private String renderDependency(String groupId, String artifactId, String version, String scope) {
        String scopeLine = scope == null ? "" : "\t\t\t<scope>" + scope + "</scope>\n";
        String versionLine = version == null ? "" : "\t\t\t<version>" + version + "</version>\n";
        return "\t\t<dependency>\n"
                + "\t\t\t<groupId>" + groupId + "</groupId>\n"
                + "\t\t\t<artifactId>" + artifactId + "</artifactId>\n"
                + versionLine
                + scopeLine
                + "\t\t</dependency>";
    }

    private void writeApplicationProperties(DatabaseEnum database, ApiEnum apiType, Path project) throws IOException {
        if (database.isDocument()) {
            String template = fileUtils.getTemplateContent("/mongo/src/main/resources/application.properties");
            String content = template
                    .replace("DATABASE_LABEL", database.getLabel())
                    .replace("DATABASE_URL", "${MONGODB_URI:" + database.getJdbcUrl() + "}")
                    .replace("GRAPHQL_PROPERTIES", graphQlProperties(apiType))
                    .replace("API_DOCS_PROPERTIES", apiDocsProperties(apiType));
            fileUtils.writeFile(project + "/src/main/resources/application.properties", content);
            return;
        }

        String template = fileUtils.getTemplateContent("/src/main/resources/application.properties");
        String content = template
                .replace("DATABASE_LABEL", database.getLabel())
                .replace("DATABASE_URL", "${DB_URL:" + database.getJdbcUrl() + "}")
                .replace("DATABASE_USERNAME", "${DB_USER:" + database.getUsername() + "}")
                .replace("DATABASE_PASSWORD", "${DB_PASSWORD:" + database.getPassword() + "}")
                .replace("DATABASE_EXTRA_PROPERTIES", extraProperties(database))
                .replace("GRAPHQL_PROPERTIES", graphQlProperties(apiType))
                .replace("API_DOCS_PROPERTIES", apiDocsProperties(apiType));
        fileUtils.writeFile(project + "/src/main/resources/application.properties", content);
    }

    private String graphQlProperties(ApiEnum apiType) {
        if (!apiType.isGraphQL()) {
            return "";
        }
        return "\n# GraphQL\nspring.graphql.graphiql.enabled=true\nspring.graphql.schema.printer.enabled=true\n";
    }

    private String apiDocsProperties(ApiEnum apiType) {
        if (apiType.isGraphQL()) {
            return "";
        }
        return "\n# OpenAPI / Swagger UI\n"
                + "springdoc.swagger-ui.path=/swagger-ui.html\n"
                + "springdoc.api-docs.path=/v3/api-docs\n";
    }

    private String extraProperties(DatabaseEnum database) {
        return switch (database) {
            case H2 -> "spring.h2.console.enabled=true\nspring.h2.console.path=/h2-console\n";
            case SQLITE -> "spring.datasource.driver-class-name=org.sqlite.JDBC\n"
                    + "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect\n";
            case MYSQL, MARIADB, POSTGRESQL, MONGODB -> "";
        };
    }

    private void writeProjectReadme(String title, DatabaseEnum database, ApiEnum apiType, Path project) throws IOException {
        if (database.isDocument()) {
            String template = fileUtils.getTemplateContent("/mongo/README.md.template");
            String content = template
                    .replace("PROJECT_NAME", title.trim())
                    .replace("DATABASE_LABEL", database.getLabel())
                    .replace("DATABASE_URL", database.getJdbcUrl())
                    .replace("API_SECTION", apiSection(apiType))
                    .replace("API_RUN_INSTRUCTIONS", apiRunInstructions(apiType));
            fileUtils.writeFile(project + "/README.md", content);
            return;
        }

        String template = fileUtils.getTemplateContent("/README.md.template");
        String runInstructions;
        if (database == DatabaseEnum.H2) {
            runInstructions = h2RunInstructions();
        } else if (database == DatabaseEnum.SQLITE) {
            runInstructions = sqliteRunInstructions();
        } else {
            runInstructions = dockerRunInstructions(database);
        }
        String content = template
                .replace("PROJECT_NAME", title.trim())
                .replace("DATABASE_LABEL", database.getLabel())
                .replace("DATABASE_URL", database.getJdbcUrl())
                .replace("DATABASE_USERNAME", database.getUsername())
                .replace("DATABASE_PASSWORD", database.getPassword())
                .replace("DATABASE_RUN_INSTRUCTIONS", runInstructions)
                .replace("API_SECTION", apiSection(apiType))
                .replace("API_RUN_INSTRUCTIONS", apiRunInstructions(apiType));
        fileUtils.writeFile(project + "/README.md", content);
    }

    private String apiSection(ApiEnum apiType) {
        if (!apiType.isGraphQL()) {
            return "## API REST\n\n"
                    + "La API REST expone un controller por entidad bajo `/api/nombreEntidad`.\n\n"
                    + "- Swagger UI: `http://localhost:8080/swagger-ui.html`\n"
                    + "- OpenAPI JSON: `http://localhost:8080/v3/api-docs`\n"
                    + "- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`\n\n"
                    + "Swagger UI permite explorar y probar los endpoints generados sin escribir las peticiones HTTP a mano.\n";
        }
        return "## API GraphQL\n\n"
                + "El proyecto expone la API en `http://localhost:8080/graphql`. El esquema está en "
                + "`src/main/resources/graphql/schema.graphqls` y las operaciones se generan a partir de las entidades.\n\n"
                + "- `findAll<Entidad>` y `find<Entidad>ById(id: ID!)`\n"
                + "- `create<Entidad>(input: <Entidad>Input!)`, `update<Entidad>(input: <Entidad>Input!)` y `delete<Entidad>(id: ID!)`\n\n"
                + "Con la aplicación en ejecución podés explorar y probar la API desde GraphiQL en "
                + "`http://localhost:8080/graphiql`. Los tipos `Date`, `DateTime`, `Time`, "
                + "`BigDecimal` y `UUID` se exponen como scalars GraphQL; `Long` se expone como `Float`.\n";
    }

    private String apiRunInstructions(ApiEnum apiType) {
        if (apiType.isGraphQL()) {
            return "La API GraphQL queda disponible en `http://localhost:8080/graphql`.";
        }
        return "La API REST queda disponible en `http://localhost:8080/api/...`.\n\n"
                + "La documentación interactiva está en `http://localhost:8080/swagger-ui.html`.";
    }

    private String h2RunInstructions() {
        return "### H2 (embebida)\n\n"
                + "La base se crea automáticamente en `./data/test`.\n\n"
                + "Consola H2: http://localhost:8080/h2-console\n\n"
                + "- JDBC URL: `jdbc:h2:file:./data/test`\n"
                + "- Usuario: `sa`\n"
                + "- Password: vacío";
    }

    private String sqliteRunInstructions() {
        return "### SQLite (archivo)\n\n"
                + "La base se crea automáticamente en `./data/test.db`.\n\n"
                + "No necesita servidor ni Docker.\n\n"
                + "La conexión usa `?foreign_keys=on` para que SQLite respete las foreign keys.";
    }

    private String dockerRunInstructions(DatabaseEnum database) {
        return "### " + database.getLabel() + " con Docker\n\n"
                + "Levantá la base con:\n\n"
                + "```bash\n"
                + "docker compose up -d\n"
                + "```\n\n"
                + "La aplicación se conecta por defecto a `" + database.getJdbcUrl() + "` "
                + "con usuario `" + database.getUsername() + "` y password `" + database.getPassword() + "`. "
                + "Podés sobreescribir con `DB_URL`, `DB_USER` y `DB_PASSWORD`.";
    }

    private void writeDockerCompose(String title, DatabaseEnum database, Path project) throws IOException {
        if (!database.hasDockerImage()) {
            return;
        }
        String template = fileUtils.getTemplateContent("/docker-compose-" + database.getKey() + ".yml.template");
        String content = template.replace("PROJECT_SLUG", StringUtils.toArtifactId(title));
        fileUtils.writeFile(project + "/docker-compose.yml", content);
    }

    private void writeDomainFile(Entity entity, DatabaseEnum database, Path project) throws IOException {
        String domainTemplate = database.isDocument() && entity.isEmbedded()
                ? "/mongo" + srcPath + "/domain/EmbeddedBase.java"
                : templatePath(database, srcPath + "/domain/DomainBase.java");
        String baseDomainFileContent = fileUtils.getTemplateContent(domainTemplate);
        final String[] domainAttributes = {""};
        final String[] imports = {""};
        entity.getAttributes().forEach((key, value) -> {
            TypeEnum type = value.getType();
            String targetType = StringUtils.capitalizeFirstCharacter(value.getCustomType());
            if (type == TypeEnum.CUSTOM) {
                domainAttributes[0] += "\n\t@ManyToOne(cascade = CascadeType.ALL)\n";
                domainAttributes[0] += "\tprivate " + targetType + " " + key + ";\n";
            } else if (type == TypeEnum.CUSTOM_ONE_TO_ONE) {
                String joinColumn = StringUtils.transformToSnakeCase(key) + "_id";
                domainAttributes[0] += "\n\t@OneToOne(cascade = CascadeType.ALL)\n";
                domainAttributes[0] += "\t@JoinColumn(name = \"" + joinColumn + "\", unique = true)\n";
                domainAttributes[0] += "\tprivate " + targetType + " " + key + ";\n";
            } else if (type == TypeEnum.CUSTOM_LIST) {
                domainAttributes[0] += "\n\t@OneToMany(cascade = CascadeType.ALL)\n";
                String joinColumn = StringUtils.transformToSnakeCase(entity.getEntityName()) + "_"
                        + StringUtils.transformToSnakeCase(key) + "_id";
                domainAttributes[0] += "\t@JoinColumn(name = \"" + joinColumn + "\")\n";
                domainAttributes[0] += "\tprivate List<" + targetType + "> " + pluralize(key) + ";\n";
                imports[0] = addListImport(imports[0]);
            } else if (type == TypeEnum.CUSTOM_MANY_TO_MANY) {
                String joinTable = manyToManyJoinTable(entity.getEntityName(), key);
                String joinColumn = StringUtils.transformToSnakeCase(entity.getEntityName()) + "_id";
                String inverseJoinColumn = StringUtils.transformToSnakeCase(value.getCustomType()) + "_id";
                domainAttributes[0] += "\n\t@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})\n";
                domainAttributes[0] += "\t@JoinTable(name = \"" + joinTable + "\",\n";
                domainAttributes[0] += "\t\tjoinColumns = @JoinColumn(name = \"" + joinColumn + "\"),\n";
                domainAttributes[0] += "\t\tinverseJoinColumns = @JoinColumn(name = \"" + inverseJoinColumn + "\"))\n";
                domainAttributes[0] += "\tprivate List<" + targetType + "> " + pluralize(key) + ";\n";
                imports[0] = addListImport(imports[0]);
            } else if (type == TypeEnum.COMPOSITION) {
                domainAttributes[0] += "\n\tprivate " + targetType + " " + key + ";\n";
            } else if (type == TypeEnum.COMPOSITION_LIST) {
                domainAttributes[0] += "\n\tprivate List<" + targetType + "> " + pluralize(key) + ";\n";
                imports[0] = addListImport(imports[0]);
            } else {
                domainAttributes[0] += "\n\tprivate " + type.javaType + " " + key + ";\n";
                imports[0] = addJavaTypeImport(imports[0], type);
            }
        });

        String domainImports = imports[0];
        if (baseDomainFileContent.contains("import java.time.LocalDateTime;")) {
            domainImports = domainImports.replace("import java.time.LocalDateTime;\n", "");
        }

        String newDomainFileContent = baseDomainFileContent.replace("TABLE_NAME", StringUtils.transformToSnakeCase(entity.getEntityName()));
        newDomainFileContent = newDomainFileContent.replace("DOMAIN_NAME", StringUtils.capitalizeFirstCharacter(entity.getEntityName()));
        newDomainFileContent = newDomainFileContent.replace("DOMAIN_ATTRIBUTES", domainAttributes[0]);
        newDomainFileContent = newDomainFileContent.replace("IMPORTS", domainImports);

        fileUtils.writeFile(project + srcPath + "/domain/" + StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + ".java", newDomainFileContent);
    }

    private void writeServiceFile(Entity entity, DatabaseEnum database, Path project) throws IOException {
        if (entity.isEmbedded()) {
            return;
        }

        String baseServiceFileContent = fileUtils.getTemplateContent(
                templatePath(database, srcPath + "/service/ServiceBase.java"));
        final String[] saveExternalEntities = {""};
        final String[] externalEntitiesServices = {""};
        final String[] validations = {""};
        final Set<String> externalServices = new HashSet<>();

        entity.getAttributes().forEach((key, value) -> {
            String propertyName = isList(value.getType())
                    ? pluralize(StringUtils.capitalizeFirstCharacter(key))
                    : StringUtils.capitalizeFirstCharacter(key);
            String getter = entity.getEntityName() + ".get" + propertyName + "()";

            if (value.getType() == TypeEnum.CUSTOM || value.getType() == TypeEnum.CUSTOM_ONE_TO_ONE) {
                String customClass = StringUtils.capitalizeFirstCharacter(value.getCustomType());
                String serviceVariable = StringUtils.lowerFirstCharacter(customClass) + "Service";
                saveExternalEntities[0] += "\n\t\tif (" + getter + " != null) {\n\t\t\t"
                        + serviceVariable + ".save(" + getter + ");\n\t\t}\n";
                if (externalServices.add(serviceVariable)) {
                    externalEntitiesServices[0] += "\n\tprivate final " + customClass + "Service " + serviceVariable + ";\n";
                }
            }

            if (value.getValidations() != null && !value.getValidations().isEmpty()) {
                value.getValidations().forEach(validation -> {
                    if (validation == null) {
                        return;
                    }
                    validations[0] += validationCode(validation, value, getter, key);
                });
            }
        });

        String newServiceFileContent = baseServiceFileContent.replace("SERVICE_DECLARATION", StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + "Service");
        newServiceFileContent = newServiceFileContent.replace("REPOSITORY_DECLARATION", StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + "Repository");
        newServiceFileContent = newServiceFileContent.replace("REPOSITORY", entity.getEntityName() + "Repository");
        newServiceFileContent = newServiceFileContent.replace("DOMAIN_DECLARATION", StringUtils.capitalizeFirstCharacter(entity.getEntityName()));
        newServiceFileContent = newServiceFileContent.replace("DOMAIN", entity.getEntityName());
        newServiceFileContent = newServiceFileContent.replace("SAVE_EXTERNAL_ENTITIES", saveExternalEntities[0]);
        newServiceFileContent = newServiceFileContent.replace("EXTERNAL_ENTITIES_SERVICES", externalEntitiesServices[0]);
        newServiceFileContent = newServiceFileContent.replace("VALIDATIONS", validations[0]);

        fileUtils.writeFile(project + srcPath + "/service/" + StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + "Service.java", newServiceFileContent);
    }

    private void writeRepositoryFile(Entity entity, DatabaseEnum database, Path project) throws IOException {
        if (entity.isEmbedded()) {
            return;
        }

        String baseRepositoryFileContent = fileUtils.getTemplateContent(
                templatePath(database, srcPath + "/repository/RepositoryBase.java"));

        String newRepositoryFileContent = baseRepositoryFileContent.replace("ENTITY", StringUtils.capitalizeFirstCharacter(entity.getEntityName()));
        newRepositoryFileContent = newRepositoryFileContent.replace("IMPORT", importProjectBasePath + ".domain." + StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + ";\n");

        fileUtils.writeFile(project + srcPath + "/repository/" + StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + "Repository.java", newRepositoryFileContent);
    }

    private void writeControllerFile(Entity entity, DatabaseEnum database, Path project) throws IOException {
        if (entity.isEmbedded()) {
            return;
        }

        String baseControllerFileContent = fileUtils.getTemplateContent(
                templatePath(database, srcPath + "/controller/ControllerBase.java"));

        String imports = importProjectBasePath + ".domain." + StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + ";\n";
        imports += importProjectBasePath + ".service." + StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + "Service;\n";

        String newControllerFileContent = baseControllerFileContent.replace("ENTITY_DECLARATION", StringUtils.capitalizeFirstCharacter(entity.getEntityName()));
        newControllerFileContent = newControllerFileContent.replace("ENTITY", entity.getEntityName());
        newControllerFileContent = newControllerFileContent.replace("IMPORTS", imports);
        newControllerFileContent = newControllerFileContent.replace("SERVICE_DECLARATION", StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + "Service " + entity.getEntityName() + "Service;\n");
        newControllerFileContent = newControllerFileContent.replace("SERVICE", entity.getEntityName() + "Service");
        newControllerFileContent = newControllerFileContent.replace("REQUEST_MAPPING", "/api/" + entity.getEntityName());

        fileUtils.writeFile(project + srcPath + "/controller/" + StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + "Controller.java", newControllerFileContent);
    }

    private void writeGraphQlControllerFile(Entity entity, DatabaseEnum database, Path project) throws IOException {
        if (entity.isEmbedded()) {
            return;
        }

        String baseControllerFileContent = fileUtils.getTemplateContent(
                srcPath + "/controller/GraphQLControllerBase.java");

        String entityClassName = StringUtils.capitalizeFirstCharacter(entity.getEntityName());
        String imports = importProjectBasePath + ".domain." + entityClassName + ";\n";
        imports += importProjectBasePath + ".service." + entityClassName + "Service;\n";

        String idExpression = database.isDocument() ? "id" : "Long.valueOf(id)";

        String newControllerFileContent = baseControllerFileContent.replace("ENTITY_DECLARATION", entityClassName);
        newControllerFileContent = newControllerFileContent.replace("ENTITY", entity.getEntityName());
        newControllerFileContent = newControllerFileContent.replace("IMPORTS", imports);
        newControllerFileContent = newControllerFileContent.replace("ID_EXPRESSION", idExpression);

        fileUtils.writeFile(project + srcPath + "/controller/" + entityClassName + "GraphQLController.java",
                newControllerFileContent);
    }

    private void writeGraphQlSchema(Domain domain, Path project) throws IOException {
        StringBuilder schema = new StringBuilder();
        schema.append("# Esquema GraphQL generado por Spring CRUD Initializr\n\n");
        schema.append("scalar Date\n");
        schema.append("scalar DateTime\n");
        schema.append("scalar Time\n");
        schema.append("scalar BigDecimal\n");
        schema.append("scalar UUID\n\n");

        for (Entity entity : domain.getEntities()) {
            schema.append(renderGraphQlType(entity)).append("\n");
            schema.append(renderGraphQlInput(entity)).append("\n");
        }

        schema.append("type Query {\n");
        for (Entity entity : domain.getEntities()) {
            if (entity.isEmbedded()) {
                continue;
            }
            String entityClassName = StringUtils.capitalizeFirstCharacter(entity.getEntityName());
            schema.append("  findAll").append(entityClassName).append(": [")
                    .append(entityClassName).append("!]!\n");
            schema.append("  find").append(entityClassName).append("ById(id: ID!): ")
                    .append(entityClassName).append("\n");
        }
        schema.append("}\n\n");

        schema.append("type Mutation {\n");
        for (Entity entity : domain.getEntities()) {
            if (entity.isEmbedded()) {
                continue;
            }
            String entityClassName = StringUtils.capitalizeFirstCharacter(entity.getEntityName());
            schema.append("  create").append(entityClassName).append("(input: ")
                    .append(entityClassName).append("Input!): ").append(entityClassName).append("!\n");
            schema.append("  update").append(entityClassName).append("(input: ")
                    .append(entityClassName).append("Input!): ").append(entityClassName).append("!\n");
            schema.append("  delete").append(entityClassName).append("(id: ID!): Boolean!\n");
        }
        schema.append("}\n");

        fileUtils.writeFile(project + "/src/main/resources/graphql/schema.graphqls", schema.toString());
    }

    private String renderGraphQlType(Entity entity) {
        String entityClassName = StringUtils.capitalizeFirstCharacter(entity.getEntityName());
        StringBuilder type = new StringBuilder("type ").append(entityClassName).append(" {\n");
        if (!entity.isEmbedded()) {
            type.append("  id: ID!\n");
            type.append("  fechaCreacion: DateTime\n");
        }
        for (Map.Entry<String, Attribute> entry : entity.getAttributes().entrySet()) {
            Attribute attribute = entry.getValue();
            String fieldName = graphQlFieldName(entry.getKey(), attribute.getType());
            type.append("  ").append(fieldName).append(": ")
                    .append(graphQlType(attribute, false)).append("\n");
        }
        return type.append("}\n").toString();
    }

    private String renderGraphQlInput(Entity entity) {
        String entityClassName = StringUtils.capitalizeFirstCharacter(entity.getEntityName());
        StringBuilder input = new StringBuilder("input ").append(entityClassName).append("Input {\n");
        if (!entity.isEmbedded()) {
            input.append("  id: ID\n");
        }
        for (Map.Entry<String, Attribute> entry : entity.getAttributes().entrySet()) {
            Attribute attribute = entry.getValue();
            String fieldName = graphQlFieldName(entry.getKey(), attribute.getType());
            input.append("  ").append(fieldName).append(": ")
                    .append(graphQlType(attribute, true)).append("\n");
        }
        return input.append("}\n").toString();
    }

    private String graphQlFieldName(String attributeName, TypeEnum type) {
        return isList(type) ? pluralize(attributeName) : attributeName;
    }

    private String graphQlType(Attribute attribute, boolean input) {
        TypeEnum type = attribute.getType();
        String scalar = switch (type) {
            case STRING -> "String";
            case INTEGER -> "Int";
            case LONG -> "Float";
            case BOOLEAN -> "Boolean";
            case DATE -> "Date";
            case DATETIME -> "DateTime";
            case TIME -> "Time";
            case DECIMAL -> "BigDecimal";
            case DOUBLE -> "Float";
            case UUID -> "UUID";
            default -> null;
        };
        if (scalar != null) {
            return scalar;
        }

        String target = StringUtils.capitalizeFirstCharacter(attribute.getCustomType()) + (input ? "Input" : "");
        return isList(type) ? "[" + target + "]" : target;
    }

    private void writeSchemaSQLFiles(Domain domain, DatabaseEnum database, Path project) throws IOException {
        if (database.isDocument()) {
            return;
        }

        String sqlFileContent = fileUtils.getTemplateContent("/doc/scripts/schema.sql");
        String sqlTestFileContent = fileUtils.getTemplateContent("/src/test/resources/schema.sql");

        List<TableDefinition> tables = new ArrayList<>();
        Map<String, TableDefinition> tablesByName = new LinkedHashMap<>();
        for (Entity entity : domain.getEntities()) {
            String tableName = StringUtils.transformToSnakeCase(entity.getEntityName());
            TableDefinition table = new TableDefinition(tableName);
            table.columns.add(new ColumnDefinition("id", null, true));
            table.columns.add(new ColumnDefinition("fecha_creacion", "TIMESTAMP"));
            tables.add(table);
            tablesByName.put(tableName, table);
        }

        List<ForeignKeyDefinition> foreignKeys = new ArrayList<>();
        int constraintIndex = 0;

        for (Entity entity : domain.getEntities()) {
            TableDefinition table = tablesByName.get(StringUtils.transformToSnakeCase(entity.getEntityName()));
            for (Map.Entry<String, Attribute> entry : entity.getAttributes().entrySet()) {
                String key = entry.getKey();
                Attribute value = entry.getValue();
                if (value.getType() == TypeEnum.CUSTOM) {
                    String column = StringUtils.transformToSnakeCase(key) + "_id";
                    table.columns.add(new ColumnDefinition(column, "BIGINT"));
                    foreignKeys.add(new ForeignKeyDefinition(table.name, column,
                            StringUtils.transformToSnakeCase(value.getCustomType()),
                            "fk_" + table.name + "_" + (++constraintIndex)));
                } else if (value.getType() == TypeEnum.CUSTOM_ONE_TO_ONE) {
                    String column = StringUtils.transformToSnakeCase(key) + "_id";
                    table.columns.add(new ColumnDefinition(column, "BIGINT UNIQUE"));
                    foreignKeys.add(new ForeignKeyDefinition(table.name, column,
                            StringUtils.transformToSnakeCase(value.getCustomType()),
                            "fk_" + table.name + "_" + (++constraintIndex)));
                } else if (value.getType() == TypeEnum.CUSTOM_LIST
                        || value.getType() == TypeEnum.CUSTOM_MANY_TO_MANY
                        || isComposition(value.getType())) {
                    continue;
                } else {
                    table.columns.add(new ColumnDefinition(StringUtils.transformToSnakeCase(key), value.getType()));
                }
            }
        }

        for (Entity owner : domain.getEntities()) {
            String ownerTable = StringUtils.transformToSnakeCase(owner.getEntityName());
            for (Map.Entry<String, Attribute> entry : owner.getAttributes().entrySet()) {
                Attribute value = entry.getValue();
                if (value.getType() != TypeEnum.CUSTOM_LIST) {
                    continue;
                }
                String targetTable = StringUtils.transformToSnakeCase(value.getCustomType());
                TableDefinition target = tablesByName.get(targetTable);
                String column = ownerTable + "_" + StringUtils.transformToSnakeCase(entry.getKey()) + "_id";
                target.columns.add(new ColumnDefinition(column, "BIGINT"));
                foreignKeys.add(new ForeignKeyDefinition(targetTable, column, ownerTable,
                        "fk_" + targetTable + "_" + (++constraintIndex)));
            }
        }

        Set<String> tableNames = new HashSet<>(tablesByName.keySet());
        for (Entity owner : domain.getEntities()) {
            String ownerTable = StringUtils.transformToSnakeCase(owner.getEntityName());
            for (Map.Entry<String, Attribute> entry : owner.getAttributes().entrySet()) {
                Attribute value = entry.getValue();
                if (value.getType() != TypeEnum.CUSTOM_MANY_TO_MANY) {
                    continue;
                }
                String targetTable = StringUtils.transformToSnakeCase(value.getCustomType());
                String joinTableName = manyToManyJoinTable(owner.getEntityName(), entry.getKey());
                if (!tableNames.add(joinTableName)) {
                    throw new IllegalArgumentException("La relación '" + entry.getKey() + "' de la entidad '"
                            + owner.getEntityName() + "' genera la tabla duplicada '" + joinTableName + "'");
                }

                TableDefinition joinTable = new TableDefinition(joinTableName);
                String ownerColumn = ownerTable + "_id";
                String targetColumn = targetTable + "_id";
                joinTable.columns.add(new ColumnDefinition(ownerColumn, "BIGINT"));
                joinTable.columns.add(new ColumnDefinition(targetColumn, "BIGINT"));
                joinTable.primaryKeyColumns.add(ownerColumn);
                joinTable.primaryKeyColumns.add(targetColumn);
                tables.add(joinTable);

                foreignKeys.add(new ForeignKeyDefinition(joinTableName, ownerColumn, ownerTable,
                        "fk_" + joinTableName + "_" + (++constraintIndex)));
                foreignKeys.add(new ForeignKeyDefinition(joinTableName, targetColumn, targetTable,
                        "fk_" + joinTableName + "_" + (++constraintIndex)));
            }
        }

        for (TableDefinition table : tables) {
            Set<String> columnNames = new HashSet<>();
            for (ColumnDefinition column : table.columns) {
                if (!columnNames.add(column.name)) {
                    throw new IllegalArgumentException("El esquema generaría la columna duplicada '" + column.name
                            + "' en la tabla '" + table.name + "'. Renombrá el atributo o la relación que la genera");
                }
            }
        }

        SqlDialect documentDialect = SqlDialect.from(database);
        String documentCreateScript = renderCreateTables(tables, foreignKeys, documentDialect)
                + "\n\n" + renderForeignKeys(foreignKeys, documentDialect) + "\n";
        if (documentDialect == SqlDialect.SQLITE) {
            documentCreateScript = "PRAGMA foreign_keys = ON;\n\n" + documentCreateScript;
        }
        String h2CreateScript = renderCreateTables(tables, foreignKeys, SqlDialect.H2)
                + "\n\n" + renderForeignKeys(foreignKeys, SqlDialect.H2) + "\n";
        String h2DropScript = renderDropTables(tables, SqlDialect.H2) + "\n\n";

        String newSqlFileContent = sqlFileContent.replace("CREATE_TABLES", documentCreateScript);
        String newSqlTestFileContent = sqlTestFileContent.replace("DROP_TABLES", h2DropScript);
        newSqlTestFileContent = newSqlTestFileContent.replace("CREATE_TABLES", h2CreateScript);

        fileUtils.writeFile(project + "/doc/scripts/schema.sql", newSqlFileContent);
        fileUtils.writeFile(project + "/src/test/resources/schema.sql", newSqlTestFileContent);
    }

    private String renderCreateTables(List<TableDefinition> tables,
                                      List<ForeignKeyDefinition> foreignKeys,
                                      SqlDialect dialect) {
        List<String> statements = new ArrayList<>();
        for (TableDefinition table : tables) {
            List<String> columns = new ArrayList<>();
            for (ColumnDefinition column : table.columns) {
                String type;
                if (column.identity) {
                    type = dialect.identityColumn;
                } else if (column.type != null) {
                    type = column.type;
                } else {
                    type = dialect.columnType(column.attributeType);
                }
                columns.add("\t" + dialect.quote(column.name) + " " + type);
            }
            if (!table.primaryKeyColumns.isEmpty()) {
                List<String> primaryKey = new ArrayList<>();
                for (String column : table.primaryKeyColumns) {
                    primaryKey.add(dialect.quote(column));
                }
                columns.add("\tPRIMARY KEY (" + String.join(", ", primaryKey) + ")");
            }
            if (dialect.inlineForeignKeys) {
                for (ForeignKeyDefinition foreignKey : foreignKeys) {
                    if (!foreignKey.table.equals(table.name)) {
                        continue;
                    }
                    columns.add("\tCONSTRAINT " + dialect.quote(foreignKey.name)
                            + " FOREIGN KEY (" + dialect.quote(foreignKey.column) + ")"
                            + " REFERENCES " + dialect.quote(foreignKey.referencedTable)
                            + "(" + dialect.quote("id") + ")");
                }
            }
            statements.add("CREATE TABLE IF NOT EXISTS " + dialect.quote(table.name) + "(\n"
                    + String.join(",\n", columns) + "\n);");
        }
        return String.join("\n\n", statements);
    }

    private String renderForeignKeys(List<ForeignKeyDefinition> foreignKeys, SqlDialect dialect) {
        if (dialect.inlineForeignKeys) {
            return "";
        }
        List<String> statements = new ArrayList<>();
        for (ForeignKeyDefinition foreignKey : foreignKeys) {
            statements.add("ALTER TABLE " + dialect.quote(foreignKey.table)
                    + " ADD CONSTRAINT " + dialect.quote(foreignKey.name)
                    + " FOREIGN KEY (" + dialect.quote(foreignKey.column) + ")"
                    + " REFERENCES " + dialect.quote(foreignKey.referencedTable)
                    + "(" + dialect.quote("id") + ");");
        }
        return String.join("\n", statements);
    }

    private String renderDropTables(List<TableDefinition> tables, SqlDialect dialect) {
        List<String> statements = new ArrayList<>();
        for (int index = tables.size() - 1; index >= 0; index--) {
            String suffix = dialect.dropCascade ? " CASCADE;" : ";";
            statements.add("DROP TABLE IF EXISTS " + dialect.quote(tables.get(index).name) + suffix);
        }
        return String.join("\n", statements);
    }

    private enum SqlDialect {
        MYSQL("`", "BIGINT AUTO_INCREMENT PRIMARY KEY", false, true),
        MARIADB("`", "BIGINT AUTO_INCREMENT PRIMARY KEY", false, true),
        POSTGRESQL("\"", "BIGSERIAL PRIMARY KEY", false, true),
        H2("\"", "BIGINT AUTO_INCREMENT PRIMARY KEY", false, true),
        SQLITE("\"", "INTEGER PRIMARY KEY AUTOINCREMENT", true, false);

        private final String quoteMark;
        private final String identityColumn;
        private final boolean inlineForeignKeys;
        private final boolean dropCascade;

        SqlDialect(String quoteMark,
                   String identityColumn,
                   boolean inlineForeignKeys,
                   boolean dropCascade) {
            this.quoteMark = quoteMark;
            this.identityColumn = identityColumn;
            this.inlineForeignKeys = inlineForeignKeys;
            this.dropCascade = dropCascade;
        }

        private String quote(String identifier) {
            return quoteMark + identifier + quoteMark;
        }

        private String columnType(TypeEnum type) {
            return switch (type) {
                case STRING -> "VARCHAR(255)";
                case INTEGER -> "INT";
                case LONG -> "BIGINT";
                case BOOLEAN -> "BOOLEAN";
                case DATE -> "DATE";
                case DATETIME -> switch (this) {
                    case MYSQL, MARIADB -> "DATETIME";
                    default -> "TIMESTAMP";
                };
                case TIME -> "TIME";
                case DECIMAL -> "DECIMAL(19,4)";
                case DOUBLE -> switch (this) {
                    case MYSQL, MARIADB -> "DOUBLE";
                    case SQLITE -> "REAL";
                    default -> "DOUBLE PRECISION";
                };
                case UUID -> switch (this) {
                    case MYSQL, MARIADB -> "CHAR(36)";
                    case SQLITE -> "TEXT";
                    default -> "UUID";
                };
                default -> throw new IllegalArgumentException("Tipo no soportado en SQL: " + type);
            };
        }

        private static SqlDialect from(DatabaseEnum database) {
            return switch (database) {
                case MYSQL -> MYSQL;
                case MARIADB -> MARIADB;
                case POSTGRESQL -> POSTGRESQL;
                case H2 -> H2;
                case SQLITE -> SQLITE;
                case MONGODB -> throw new IllegalArgumentException("MongoDB no usa un esquema SQL");
            };
        }
    }

    private static final class TableDefinition {
        private final String name;
        private final List<ColumnDefinition> columns = new ArrayList<>();
        private final List<String> primaryKeyColumns = new ArrayList<>();

        private TableDefinition(String name) {
            this.name = name;
        }
    }

    private static final class ColumnDefinition {
        private final String name;
        private final String type;
        private final TypeEnum attributeType;
        private final boolean identity;

        private ColumnDefinition(String name, String type) {
            this(name, type, null, false);
        }

        private ColumnDefinition(String name, TypeEnum attributeType) {
            this(name, null, attributeType, false);
        }

        private ColumnDefinition(String name, String type, boolean identity) {
            this(name, type, null, identity);
        }

        private ColumnDefinition(String name, String type, TypeEnum attributeType, boolean identity) {
            this.name = name;
            this.type = type;
            this.attributeType = attributeType;
            this.identity = identity;
        }
    }

    private static final class ForeignKeyDefinition {
        private final String table;
        private final String column;
        private final String referencedTable;
        private final String name;

        private ForeignKeyDefinition(String table, String column, String referencedTable, String name) {
            this.table = table;
            this.column = column;
            this.referencedTable = referencedTable;
            this.name = name;
        }
    }

    private String pluralize(String value) {
        if (value == null || value.isEmpty() || value.endsWith("s")) {
            return value;
        }
        if (value.endsWith("z")) {
            return value.substring(0, value.length() - 1) + "ces";
        }
        return value + "s";
    }

    private String manyToManyJoinTable(String ownerEntity, String attributeName) {
        return StringUtils.transformToSnakeCase(ownerEntity) + "_"
                + StringUtils.transformToSnakeCase(attributeName);
    }

    private String addListImport(String imports) {
        if (imports.contains("import java.util.List;")) {
            return imports;
        }
        return imports + "import java.util.List;\n";
    }

    private String addJavaTypeImport(String imports, TypeEnum type) {
        String importLine = switch (type) {
            case DATE -> "import java.time.LocalDate;\n";
            case DATETIME -> "import java.time.LocalDateTime;\n";
            case TIME -> "import java.time.LocalTime;\n";
            case DECIMAL -> "import java.math.BigDecimal;\n";
            case UUID -> "import java.util.UUID;\n";
            default -> "";
        };
        if (importLine.isEmpty() || imports.contains(importLine)) {
            return imports;
        }
        return imports + importLine;
    }

    private String validationCode(ValidationEnum validation, Attribute attribute, String getter, String key) {
        String message = key + " " + validation.message;
        if (validation == ValidationEnum.MIN || validation == ValidationEnum.MAX) {
            BigDecimal bound = validation == ValidationEnum.MIN
                    ? attribute.getMinValue()
                    : attribute.getMaxValue();
            message += " " + bound.toPlainString();
        }
        return switch (validation) {
            case NOT_NULL -> "\n\tAssert.notNull(" + getter + ", \"" + message + "\");\n";
            case NOT_BLANK -> "\n\tAssert.isTrue(" + getter + " != null && !" + getter
                    + ".isBlank(), \"" + message + "\");\n";
            case NOT_EMPTY -> "\n\tAssert.notEmpty(" + getter + ", \"" + message + "\");\n";
            case PAST -> temporalValidation(getter, attribute.getType(), "isBefore", false, message);
            case PAST_OR_PRESENT -> temporalValidation(getter, attribute.getType(), "isAfter", true, message);
            case FUTURE -> temporalValidation(getter, attribute.getType(), "isAfter", false, message);
            case FUTURE_OR_PRESENT -> temporalValidation(getter, attribute.getType(), "isBefore", true, message);
            case POSITIVE -> numericValidation(getter, attribute.getType(), ">", BigDecimal.ZERO, message);
            case NEGATIVE -> numericValidation(getter, attribute.getType(), "<", BigDecimal.ZERO, message);
            case POSITIVE_OR_ZERO -> numericValidation(getter, attribute.getType(), ">=", BigDecimal.ZERO, message);
            case NEGATIVE_OR_ZERO -> numericValidation(getter, attribute.getType(), "<=", BigDecimal.ZERO, message);
            case MIN -> numericValidation(getter, attribute.getType(), ">=", attribute.getMinValue(), message);
            case MAX -> numericValidation(getter, attribute.getType(), "<=", attribute.getMaxValue(), message);
        };
    }

    private String temporalValidation(String getter, TypeEnum type, String method, boolean negate, String message) {
        String now = switch (type) {
            case DATE -> "java.time.LocalDate.now()";
            case DATETIME -> "java.time.LocalDateTime.now()";
            case TIME -> "java.time.LocalTime.now()";
            default -> throw new IllegalArgumentException("Validación temporal no soportada para el tipo " + type);
        };
        String condition = getter + "." + method + "(" + now + ")";
        if (negate) {
            condition = "!" + condition;
        }
        return "\n\tAssert.isTrue(" + getter + " == null || " + condition + ", \"" + message + "\");\n";
    }

    private String numericValidation(String getter, TypeEnum type, String operator, BigDecimal bound, String message) {
        String comparison;
        if (type == TypeEnum.DECIMAL) {
            String boundExpression = "java.math.BigDecimal.ZERO";
            if (bound != null && bound.compareTo(BigDecimal.ZERO) != 0) {
                boundExpression = "new java.math.BigDecimal(\"" + bound.toPlainString() + "\")";
            }
            comparison = getter + ".compareTo(" + boundExpression + ") " + operator + " 0";
        } else {
            comparison = getter + " " + operator + " " + numericLiteral(type, bound);
        }
        return "\n\tAssert.isTrue(" + getter + " == null || " + comparison + ", \"" + message + "\");\n";
    }

    private String numericLiteral(TypeEnum type, BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return switch (type) {
            case INTEGER -> String.valueOf(safeValue.intValueExact());
            case LONG -> safeValue.longValueExact() + "L";
            case DOUBLE -> safeValue.toPlainString();
            default -> throw new IllegalArgumentException("Tipo numérico no soportado: " + type);
        };
    }

    private String defaultValue(Attribute attribute) {
        TypeEnum type = attribute.getType();
        List<ValidationEnum> validations = attribute.getValidations() == null
                ? List.of()
                : attribute.getValidations();
        return switch (type) {
            case STRING -> "\"test\"";
            case INTEGER -> String.valueOf(numericDefaultValue(attribute).intValueExact());
            case LONG -> numericDefaultValue(attribute).longValueExact() + "L";
            case BOOLEAN -> "true";
            case DATE -> temporalDateDefault(validations, true);
            case DATETIME -> temporalDateDefault(validations, false);
            case TIME -> validations.contains(ValidationEnum.FUTURE)
                    || validations.contains(ValidationEnum.FUTURE_OR_PRESENT)
                    ? "LocalTime.MAX"
                    : validations.contains(ValidationEnum.PAST)
                    || validations.contains(ValidationEnum.PAST_OR_PRESENT)
                    ? "LocalTime.MIN"
                    : "LocalTime.of(12, 0)";
            case DECIMAL -> "new BigDecimal(\""
                    + numericDefaultValue(attribute).toPlainString() + "\")";
            case DOUBLE -> String.valueOf(numericDefaultValue(attribute).doubleValue());
            case UUID -> "UUID.fromString(\"00000000-0000-0000-0000-000000000001\")";
            default -> "null";
        };
    }

    private String temporalDateDefault(List<ValidationEnum> validations, boolean dateOnly) {
        String now = dateOnly ? "LocalDate.now()" : "LocalDateTime.now()";
        if (validations.contains(ValidationEnum.FUTURE)
                || validations.contains(ValidationEnum.FUTURE_OR_PRESENT)) {
            return now + ".plusDays(1)";
        }
        if (validations.contains(ValidationEnum.PAST)
                || validations.contains(ValidationEnum.PAST_OR_PRESENT)) {
            return now + ".minusDays(1)";
        }
        return dateOnly ? "LocalDate.of(2024, 1, 1)" : "LocalDateTime.of(2024, 1, 1, 0, 0)";
    }

    private BigDecimal numericDefaultValue(Attribute attribute) {
        List<ValidationEnum> validations = attribute.getValidations() == null
                ? List.of()
                : attribute.getValidations();
        BigDecimal lower = attribute.getMinValue();
        BigDecimal upper = attribute.getMaxValue();

        if (validations.contains(ValidationEnum.POSITIVE)) {
            lower = max(lower, BigDecimal.ONE);
        }
        if (validations.contains(ValidationEnum.POSITIVE_OR_ZERO)) {
            lower = max(lower, BigDecimal.ZERO);
        }
        if (validations.contains(ValidationEnum.NEGATIVE)) {
            upper = min(upper, BigDecimal.valueOf(-1));
        }
        if (validations.contains(ValidationEnum.NEGATIVE_OR_ZERO)) {
            upper = min(upper, BigDecimal.ZERO);
        }

        if (lower != null) {
            return lower;
        }
        if (upper != null) {
            return upper;
        }
        return BigDecimal.ONE;
    }

    private BigDecimal max(BigDecimal current, BigDecimal candidate) {
        return current == null || current.compareTo(candidate) < 0 ? candidate : current;
    }

    private BigDecimal min(BigDecimal current, BigDecimal candidate) {
        return current == null || current.compareTo(candidate) > 0 ? candidate : current;
    }

    private void writeBuilderFile(Entity entity, Path project) throws IOException {
        if (entity.isEmbedded()) {
            return;
        }

        String builderFile = fileUtils.getTemplateContent(testPath + "/builder/BuilderBase.java");
        String[] genericMethods = {""};
        String[] genericBuild = {""};
        String domainEntity = StringUtils.capitalizeFirstCharacter(entity.getEntityName());
        String[] importString = {importProjectBasePath + ".domain." + domainEntity + ";\n"};

        entity.getAttributes().forEach((key, value) -> {
            String attribute = StringUtils.capitalizeFirstCharacter(key);
            TypeEnum typeEnum = value.getType();
            String propertyName = isList(typeEnum) ? pluralize(attribute) : attribute;
            String type = typeEnum.javaType;
            if (isLink(typeEnum)) {
                String customType = StringUtils.capitalizeFirstCharacter(value.getCustomType());
                String customImport = importProjectBasePath + ".domain." + customType + ";\n";
                if (!importString[0].contains(customImport)) {
                    importString[0] += customImport;
                }
                type = isList(typeEnum) ? "List<" + customType + ">" : customType;
                if (isList(typeEnum)) {
                    importString[0] = addListImport(importString[0]);
                }
            } else {
                importString[0] = addJavaTypeImport(importString[0], typeEnum);
            }
            genericMethods[0] += "public " + domainEntity + "Builder with" + propertyName + "(" + type + " val) {\n\t";
            genericMethods[0] += "instance.set" + propertyName + "(val);\n\treturn this;\n}\n\n";
            genericBuild[0] += "builder.with" + propertyName + "(" + defaultValue(value) + ");\n\t";
        });

        String newBuilderFile = builderFile.replace("DOMAIN", domainEntity);
        newBuilderFile = newBuilderFile.replace("IMPORT", importString[0]);
        newBuilderFile = newBuilderFile.replace("GENERIC_BUILD", genericBuild[0]);
        newBuilderFile = newBuilderFile.replace("GENERIC_METHODS", genericMethods[0]);

        fileUtils.writeFile(project + testPath + "/builder/" + domainEntity + "Builder.java", newBuilderFile);
    }

    private void writeTestFile(Entity entity, DatabaseEnum database, Path project) throws IOException {
        if (entity.isEmbedded()) {
            return;
        }

        String domainEntity = StringUtils.capitalizeFirstCharacter(entity.getEntityName());
        String testFile = fileUtils.getTemplateContent(
                templatePath(database, testPath + "/tests/service/ServiceTestBase.java"));

        String newTestFile = testFile.replace("DOMAIN_DECLARATION", domainEntity);
        newTestFile = newTestFile.replace("DOMAIN", entity.getEntityName());
        newTestFile = newTestFile.replace("SERVICE_DECLARATION", domainEntity + "Service");
        newTestFile = newTestFile.replace("SERVICE", entity.getEntityName() + "Service");

        fileUtils.writeFile(project + testPath + "/tests/service/" + domainEntity + "ServiceTest.java", newTestFile);
    }

    private String escapeJavaString(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static boolean isJavaIdentifier(String value) {
        if (value == null || value.isEmpty() || JAVA_KEYWORDS.contains(value)) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(value.charAt(0))) {
            return false;
        }
        return value.codePoints().skip(1).allMatch(Character::isJavaIdentifierPart);
    }
}
