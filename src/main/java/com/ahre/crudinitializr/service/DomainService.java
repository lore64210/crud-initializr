package com.ahre.crudinitializr.service;

import com.ahre.crudinitializr.enums.ApiEnum;
import com.ahre.crudinitializr.enums.DatabaseEnum;
import com.ahre.crudinitializr.enums.TypeEnum;
import com.ahre.crudinitializr.enums.ValidationEnum;
import com.ahre.crudinitializr.utils.FileUtils;
import com.ahre.crudinitializr.utils.StringUtils;
import com.ahre.crudinitializr.vo.Attribute;
import com.ahre.crudinitializr.vo.CacheSettings;
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
import java.util.stream.Collectors;

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
        CacheSettings cache = domain.getCache() == null ? new CacheSettings() : domain.getCache();
        validateDatabaseCompatibility(domain, database);
        validateApiCompatibility(domain, apiType);
        validateCacheSettings(domain, cache);

        Path project = null;
        String mainClass = StringUtils.toClassName(domain.getTitle()) + "Application";
        String databaseName = StringUtils.toDatabaseName(domain.getTitle());
        try {
            project = fileUtils.createOutputFolder();
            writeStaticFiles(project, database, apiType, mainClass);
            writeOpenApiConfig(domain.getTitle(), apiType, project);
            writeCacheConfig(cache, project);
            writeProjectReadme(domain.getTitle(), domain, database, apiType, cache, project);
            writeDockerCompose(domain.getTitle(), database, cache, project);
            writePomFile(domain.getTitle(), database, apiType, cache, project);
            writeApplicationProperties(database, apiType, cache, databaseName, project);
            for (Entity entity : domain.getEntities()) {
                writeDomainFile(entity, database, project);
                writeServiceFile(entity, database, cache, project);
                writeRepositoryFile(entity, database, project);
                if (!database.isDocument() && !entity.isEmbedded()) {
                    writeRequestFile(entity, project);
                    writeMapperFile(entity, project);
                }
                if (apiType.isGraphQL()) {
                    writeGraphQlControllerFile(entity, database, project);
                } else {
                    writeControllerFile(entity, database, project);
                }
                writeBuilderFile(entity, domain, project);
                writeTestFile(entity, database, project);
            }
            if (apiType.isGraphQL()) {
                writeGraphQlSchema(domain, database, project);
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

    private void validateCacheSettings(Domain domain, CacheSettings cache) {
        if (!cache.isEnabled()) {
            return;
        }

        String prefix = cache.getPrefix();
        Assert.isTrue(prefix != null && !prefix.trim().isEmpty(),
                "El prefijo de claves de Redis no puede estar vacío");
        prefix = prefix.trim();
        Assert.isTrue(prefix.length() <= 50,
                "El prefijo de claves de Redis no puede tener más de 50 caracteres");
        Assert.isTrue(prefix.matches("[A-Za-z0-9:_-]+"),
                "El prefijo de claves de Redis solo puede contener letras, números, ':', '_' y '-'");
        Assert.isTrue(cache.getTtlSeconds() > 0,
                "El TTL de Redis debe ser mayor que cero");
        Assert.isTrue(cache.getTtlSeconds() <= 31_536_000,
                "El TTL de Redis no puede superar un año");
        Assert.isTrue(domain.getEntities().stream()
                        .anyMatch(entity -> !entity.isEmbedded() && entity.isCacheEnabled()),
                "Seleccioná al menos una entidad para cachear o deshabilitá el cache Redis");
        cache.setPrefix(prefix);
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

    private void writeStaticFiles(Path project,
                                  DatabaseEnum database,
                                  ApiEnum apiType,
                                  String mainClass) throws IOException {
        writeMainClass(project, database, mainClass);

        if (database.isDocument()) {
            fileUtils.copyTemplate(templatePath(database, testPath + "/tests/ApplicationTests.java"),
                    project, destinationPath(testPath + "/tests/ApplicationTests.java"));
            fileUtils.copyTemplate("/mongo/src/test/resources/application.properties",
                    project, "src/test/resources/application.properties");
        } else {
            List<String> templates = List.of(
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

    private void writeMainClass(Path project, DatabaseEnum database, String mainClass) throws IOException {
        String content = fileUtils.getTemplateContent(
                        templatePath(database, srcPath + "/ProjectBaseApplication.java"))
                .replace("ProjectBaseApplication", mainClass);
        fileUtils.writeFile(project + "/" + destinationPath(srcPath + "/" + mainClass + ".java"), content);
    }

    private void writeCacheConfig(CacheSettings cache, Path project) throws IOException {
        if (!cache.isEnabled()) {
            return;
        }
        fileUtils.copyTemplate(srcPath + "/config/CacheConfig.java", project);
        fileUtils.copyTemplate(srcPath + "/config/LoggingCacheErrorHandler.java", project);
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

    private void writePomFile(String title,
                              DatabaseEnum database,
                              ApiEnum apiType,
                              CacheSettings cache,
                              Path project) throws IOException {
        String content = fileUtils.getTemplateContent("/pom.xml");
        String escapedTitle = StringUtils.escapeXml(title.trim());
        content = content.replace("PROJECT_ARTIFACT_ID", StringUtils.toArtifactId(title));
        content = content.replace("PROJECT_NAME", escapedTitle);
        content = content.replace("PROJECT_DESCRIPTION", escapedTitle);
        content = content.replace("DATABASE_LABEL", database.getLabel());
        content = content.replace("PERSISTENCE_DEPENDENCY", renderPersistenceDependency(database));
        content = content.replace("CACHE_DEPENDENCY", cache.isEnabled()
                ? renderCacheDependencies()
                : "");
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

    private String renderCacheDependencies() {
        return renderDependency("org.springframework.boot", "spring-boot-starter-data-redis", null)
                + "\n" + renderDependency("org.springframework.boot", "spring-boot-starter-cache", null);
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

    private void writeApplicationProperties(DatabaseEnum database,
                                            ApiEnum apiType,
                                            CacheSettings cache,
                                            String databaseName,
                                            Path project) throws IOException {
        if (database.isDocument()) {
            String template = fileUtils.getTemplateContent("/mongo/src/main/resources/application.properties");
            String content = template
                    .replace("DATABASE_LABEL", database.getLabel())
                    .replace("DATABASE_URL", "${MONGODB_URI:" + database.jdbcUrl(databaseName) + "}")
                    .replace("GRAPHQL_PROPERTIES", graphQlProperties(apiType))
                    .replace("API_DOCS_PROPERTIES", apiDocsProperties(apiType))
                    .replace("CACHE_PROPERTIES", cacheProperties(cache));
            fileUtils.writeFile(project + "/src/main/resources/application.properties", content);
            return;
        }

        String template = fileUtils.getTemplateContent("/src/main/resources/application.properties");
        String content = template
                .replace("DATABASE_LABEL", database.getLabel())
                .replace("DATABASE_URL", "${DB_URL:" + database.jdbcUrl(databaseName) + "}")
                .replace("DATABASE_USERNAME", "${DB_USER:" + database.getUsername() + "}")
                .replace("DATABASE_PASSWORD", "${DB_PASSWORD:" + database.getPassword() + "}")
                .replace("DATABASE_EXTRA_PROPERTIES", extraProperties(database))
                .replace("GRAPHQL_PROPERTIES", graphQlProperties(apiType))
                .replace("API_DOCS_PROPERTIES", apiDocsProperties(apiType))
                .replace("CACHE_PROPERTIES", cacheProperties(cache));
        fileUtils.writeFile(project + "/src/main/resources/application.properties", content);
    }

    private String graphQlProperties(ApiEnum apiType) {
        if (!apiType.isGraphQL()) {
            return "";
        }
        return "\n# GraphQL\nspring.graphql.graphiql.enabled=true\nspring.graphql.schema.printer.enabled=true\n";
    }

    private String cacheProperties(CacheSettings cache) {
        if (!cache.isEnabled()) {
            return "";
        }
        return "\n# Cache Redis\n"
                + "app.cache.enabled=true\n"
                + "app.cache.prefix=" + cache.getPrefix() + "\n"
                + "app.cache.ttl-seconds=" + cache.getTtlSeconds() + "\n"
                + "spring.cache.type=redis\n"
                + "spring.data.redis.url=${REDIS_URL:redis://localhost:6379}\n";
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

    private void writeProjectReadme(String title,
                                    Domain domain,
                                    DatabaseEnum database,
                                    ApiEnum apiType,
                                    CacheSettings cache,
                                    Path project) throws IOException {
        String databaseName = StringUtils.toDatabaseName(title);
        String mainClass = StringUtils.toClassName(title) + "Application";
        if (database.isDocument()) {
            String template = fileUtils.getTemplateContent("/mongo/README.md.template");
            String content = template
                    .replace("PROJECT_NAME", title.trim())
                    .replace("PROJECT_CLASS", mainClass)
                    .replace("DATABASE_LABEL", database.getLabel())
                    .replace("DATABASE_URL", database.jdbcUrl(databaseName))
                    .replace("API_SECTION", apiSection(apiType) + "\n\n" + relationPayloadNote(database))
                    .replace("CACHE_SECTION", cacheSection(domain, cache))
                    .replace("API_RUN_INSTRUCTIONS", apiRunInstructions(apiType));
            fileUtils.writeFile(project + "/README.md", content);
            return;
        }

        String template = fileUtils.getTemplateContent("/README.md.template");
        String runInstructions;
        if (database == DatabaseEnum.H2) {
            runInstructions = h2RunInstructions(databaseName);
        } else if (database == DatabaseEnum.SQLITE) {
            runInstructions = sqliteRunInstructions(databaseName);
        } else {
            runInstructions = dockerRunInstructions(database, databaseName);
        }
        String content = template
                .replace("PROJECT_NAME", title.trim())
                .replace("PROJECT_CLASS", mainClass)
                .replace("DATABASE_LABEL", database.getLabel())
                .replace("DATABASE_URL", database.jdbcUrl(databaseName))
                .replace("DATABASE_USERNAME", database.getUsername())
                .replace("DATABASE_PASSWORD", database.getPassword())
                .replace("DATABASE_RUN_INSTRUCTIONS", runInstructions)
                .replace("API_SECTION", apiSection(apiType) + "\n\n" + relationPayloadNote(database))
                .replace("CACHE_SECTION", cacheSection(domain, cache))
                .replace("API_RUN_INSTRUCTIONS", apiRunInstructions(apiType));
        fileUtils.writeFile(project + "/README.md", content);
    }

    private String cacheSection(Domain domain, CacheSettings cache) {
        if (!cache.isEnabled()) {
            return "";
        }
        String cachedEntities = domain.getEntities().stream()
                .filter(entity -> !entity.isEmbedded() && entity.isCacheEnabled())
                .map(entity -> "`" + entity.getEntityName() + "`")
                .collect(Collectors.joining(", "));
        return "## Cache Redis\n\n"
                + "El proyecto incluye cache Redis con prefijo `" + cache.getPrefix()
                + "` y TTL de " + cache.getTtlSeconds() + " segundos.\n\n"
                + "- Entidades cacheadas: " + cachedEntities + ".\n"
                + "- `findById` se cachea siempre.\n"
                + (cache.isCacheFindAll() ? "- `findAll` también se cachea.\n" : "- `findAll` no se cachea.\n")
                + "- `save`, `edit` y `delete` invalidan o actualizan el cache.\n\n"
                + "Levantá Redis con `docker compose up -d`. La conexión usa "
                + "`REDIS_URL`, por defecto `redis://localhost:6379`.\n";
    }

    private String relationPayloadNote(DatabaseEnum database) {
        if (database.isDocument()) {
            return "En MongoDB las composiciones (`COMPOSITION` y `COMPOSITION_LIST`) se envían como objetos anidados.";
        }
        return "En SQL las relaciones se envían por ID: `<atributo>Id` para N:1/1:1 y `<atributo>Ids` para listas.";
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

    private String h2RunInstructions(String databaseName) {
        return "### H2 (embebida)\n\n"
                + "La base se crea automáticamente en `./data/" + databaseName + "`.\n\n"
                + "Consola H2: http://localhost:8080/h2-console\n\n"
                + "- JDBC URL: `jdbc:h2:file:./data/" + databaseName + "`\n"
                + "- Usuario: `sa`\n"
                + "- Password: vacío";
    }

    private String sqliteRunInstructions(String databaseName) {
        return "### SQLite (archivo)\n\n"
                + "La base se crea automáticamente en `./data/" + databaseName + ".db`.\n\n"
                + "No necesita servidor ni Docker.\n\n"
                + "La conexión usa `?foreign_keys=on` para que SQLite respete las foreign keys.";
    }

    private String dockerRunInstructions(DatabaseEnum database, String databaseName) {
        return "### " + database.getLabel() + " con Docker\n\n"
                + "Levantá la base con:\n\n"
                + "```bash\n"
                + "docker compose up -d\n"
                + "```\n\n"
                + "La aplicación se conecta por defecto a `" + database.jdbcUrl(databaseName) + "` "
                + "con usuario `" + database.getUsername() + "` y password `" + database.getPassword() + "`. "
                + "Podés sobreescribir con `DB_URL`, `DB_USER` y `DB_PASSWORD`.";
    }

    private void writeDockerCompose(String title,
                                    DatabaseEnum database,
                                    CacheSettings cache,
                                    Path project) throws IOException {
        String slug = StringUtils.toArtifactId(title);
        if (!database.hasDockerImage()) {
            if (!cache.isEnabled()) {
                return;
            }
            String content = "services:\n" + redisService(slug) + "\nvolumes:\n" + redisVolume();
            fileUtils.writeFile(project + "/docker-compose.yml", content);
            return;
        }
        String template = fileUtils.getTemplateContent("/docker-compose-" + database.getKey() + ".yml.template");
        String databaseName = StringUtils.toDatabaseName(title);
        String content = template
                .replace("PROJECT_SLUG", slug)
                .replace("DATABASE_NAME", databaseName)
                .replace("REDIS_SERVICE", cache.isEnabled() ? redisService(slug) : "")
                .replace("REDIS_VOLUME", cache.isEnabled() ? redisVolume() : "");
        fileUtils.writeFile(project + "/docker-compose.yml", content);
    }

    private String redisService(String slug) {
        return "  redis:\n"
                + "    image: redis:7-alpine\n"
                + "    container_name: " + slug + "-redis\n"
                + "    ports:\n"
                + "      - \"6379:6379\"\n"
                + "    volumes:\n"
                + "      - redis_data:/data\n";
    }

    private String redisVolume() {
        return "  redis_data:";
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

    private void writeServiceFile(Entity entity,
                                  DatabaseEnum database,
                                  CacheSettings cache,
                                  Path project) throws IOException {
        if (entity.isEmbedded()) {
            return;
        }

        String baseServiceFileContent = fileUtils.getTemplateContent(
                templatePath(database, srcPath + "/service/ServiceBase.java"));
        final String[] validations = {""};
        final String[] relationRepositories = {""};
        final String[] relationImports = {""};
        final String[] attachRelations = {""};
        final String[] arrayListImport = {""};
        final Set<String> addedRepositories = new HashSet<>();
        final boolean document = database.isDocument();

        entity.getAttributes().forEach((key, value) -> {
            String propertyName = isList(value.getType())
                    ? pluralize(StringUtils.capitalizeFirstCharacter(key))
                    : StringUtils.capitalizeFirstCharacter(key);
            String getter = entity.getEntityName() + ".get" + propertyName + "()";
            String domain = entity.getEntityName();

            if (!document && isLink(value.getType())) {
                String target = StringUtils.capitalizeFirstCharacter(value.getCustomType());
                String repositoryVariable = StringUtils.lowerFirstCharacter(target) + "Repository";
                String targetImport = importProjectBasePath + ".domain." + target + ";\n";
                String repositoryImport = importProjectBasePath + ".repository." + target + "Repository;\n";

                if (!relationImports[0].contains(targetImport)) {
                    relationImports[0] += targetImport;
                }
                if (!relationImports[0].contains(repositoryImport)) {
                    relationImports[0] += repositoryImport;
                }
                if (addedRepositories.add(repositoryVariable)) {
                    relationRepositories[0] += "\tprivate final " + target + "Repository "
                            + repositoryVariable + ";\n";
                }

                String setter = "set" + propertyName;
                if (isList(value.getType())) {
                    arrayListImport[0] = "import java.util.ArrayList;\n";
                    String listVariable = StringUtils.lowerFirstCharacter(propertyName) + "Value";
                    String attachedVariable = StringUtils.lowerFirstCharacter(propertyName) + "Attached";
                    String itemVariable = StringUtils.lowerFirstCharacter(target) + "Value";

                    attachRelations[0] += "\t\tList<" + target + "> " + listVariable
                            + " = " + domain + ".get" + propertyName + "();\n";
                    attachRelations[0] += "\t\tif (" + listVariable + " != null) {\n";
                    attachRelations[0] += "\t\t\tList<" + target + "> " + attachedVariable
                            + " = new ArrayList<>();\n";
                    attachRelations[0] += "\t\t\tfor (" + target + " " + itemVariable
                            + " : " + listVariable + ") {\n";
                    attachRelations[0] += "\t\t\t\tif (" + itemVariable + ".getId() == null) {\n";
                    attachRelations[0] += "\t\t\t\t\t" + attachedVariable + ".add(" + itemVariable + ");\n";
                    attachRelations[0] += "\t\t\t\t} else {\n";
                    attachRelations[0] += "\t\t\t\t\t" + attachedVariable + ".add("
                            + repositoryVariable + ".findById(" + itemVariable + ".getId())\n";
                    attachRelations[0] += "\t\t\t\t\t\t\t.orElseThrow(() -> new IllegalArgumentException(\""
                            + target + " not found with id: \" + " + itemVariable + ".getId())));\n";
                    attachRelations[0] += "\t\t\t\t}\n";
                    attachRelations[0] += "\t\t\t}\n";
                    attachRelations[0] += "\t\t\t" + domain + "." + setter + "(" + attachedVariable + ");\n";
                    attachRelations[0] += "\t\t}\n\n";
                } else {
                    String relationVariable = StringUtils.lowerFirstCharacter(propertyName) + "Value";

                    attachRelations[0] += "\t\t" + target + " " + relationVariable
                            + " = " + domain + ".get" + propertyName + "();\n";
                    attachRelations[0] += "\t\tif (" + relationVariable + " != null && "
                            + relationVariable + ".getId() != null) {\n";
                    attachRelations[0] += "\t\t\t" + domain + "." + setter + "("
                            + repositoryVariable + ".findById(" + relationVariable + ".getId())\n";
                    attachRelations[0] += "\t\t\t\t\t.orElseThrow(() -> new IllegalArgumentException(\""
                            + target + " not found with id: \" + " + relationVariable + ".getId())));\n";
                    attachRelations[0] += "\t\t}\n\n";
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

        String cacheName = StringUtils.transformToSnakeCase(entity.getEntityName());
        String listCacheName = cacheName + "-list";
        String cacheImports = "";
        String cacheFindAll = "";
        String cacheFindById = "";
        String cacheSave = "";
        String cacheEdit = "";
        String cacheDelete = "";

        if (cache.isEnabled() && entity.isCacheEnabled()) {
            cacheImports = "import org.springframework.cache.annotation.CacheEvict;\n"
                    + "import org.springframework.cache.annotation.CachePut;\n"
                    + "import org.springframework.cache.annotation.Cacheable;\n"
                    + "import org.springframework.cache.annotation.Caching;\n";
            cacheFindById = "@Cacheable(cacheNames = \"" + cacheName + "\", key = \"#id\")";
            if (cache.isCacheFindAll()) {
                cacheFindAll = "@Cacheable(cacheNames = \"" + listCacheName + "\", key = \"'all'\")";
                cacheSave = "@Caching(put = @CachePut(cacheNames = \"" + cacheName + "\", key = \"#result.id\"), "
                        + "evict = @CacheEvict(cacheNames = \"" + listCacheName + "\", allEntries = true))";
                cacheEdit = cacheSave;
                cacheDelete = "@Caching(evict = {@CacheEvict(cacheNames = \"" + cacheName + "\", key = \"#id\"), "
                        + "@CacheEvict(cacheNames = \"" + listCacheName + "\", allEntries = true)})";
            } else {
                cacheSave = "@CachePut(cacheNames = \"" + cacheName + "\", key = \"#result.id\")";
                cacheEdit = cacheSave;
                cacheDelete = "@CacheEvict(cacheNames = \"" + cacheName + "\", key = \"#id\")";
            }
        }

        String newServiceFileContent = baseServiceFileContent.replace("SERVICE_DECLARATION", StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + "Service");
        newServiceFileContent = newServiceFileContent.replace("REPOSITORY_DECLARATION", StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + "Repository");
        newServiceFileContent = newServiceFileContent.replace("REPOSITORY", entity.getEntityName() + "Repository");
        newServiceFileContent = newServiceFileContent.replace("DOMAIN_DECLARATION", StringUtils.capitalizeFirstCharacter(entity.getEntityName()));
        newServiceFileContent = newServiceFileContent.replace("DOMAIN", entity.getEntityName());
        newServiceFileContent = newServiceFileContent.replace("SAVE_EXTERNAL_ENTITIES", "");
        newServiceFileContent = newServiceFileContent.replace("EXTERNAL_ENTITIES_SERVICES", "");
        newServiceFileContent = newServiceFileContent.replace("RELATION_IMPORTS", relationImports[0]);
        newServiceFileContent = newServiceFileContent.replace("RELATION_REPOSITORIES", relationRepositories[0]);
        newServiceFileContent = newServiceFileContent.replace("ARRAY_LIST_IMPORT", arrayListImport[0]);
        newServiceFileContent = newServiceFileContent.replace("ATTACH_RELATIONS", attachRelations[0]);
        newServiceFileContent = newServiceFileContent.replace("VALIDATIONS", validations[0]);
        newServiceFileContent = newServiceFileContent.replace("CACHE_IMPORTS", cacheImports);
        newServiceFileContent = newServiceFileContent.replace("CACHE_FIND_ALL", cacheFindAll);
        newServiceFileContent = newServiceFileContent.replace("CACHE_FIND_BY_ID", cacheFindById);
        newServiceFileContent = newServiceFileContent.replace("CACHE_SAVE", cacheSave);
        newServiceFileContent = newServiceFileContent.replace("CACHE_EDIT", cacheEdit);
        newServiceFileContent = newServiceFileContent.replace("CACHE_DELETE", cacheDelete);

        fileUtils.writeFile(project + srcPath + "/service/" + StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + "Service.java", newServiceFileContent);
    }

    private void writeRequestFile(Entity entity, Path project) throws IOException {
        String template = fileUtils.getTemplateContent(srcPath + "/dto/RequestBase.java");
        String entityClass = StringUtils.capitalizeFirstCharacter(entity.getEntityName());
        String[] imports = {""};
        StringBuilder fields = new StringBuilder();

        entity.getAttributes().forEach((key, value) -> {
            if (isLink(value.getType())) {
                if (isList(value.getType())) {
                    fields.append("\tprivate List<Long> ").append(pluralize(key)).append("Ids;\n\n");
                    imports[0] = addListImport(imports[0]);
                } else {
                    fields.append("\tprivate Long ").append(key).append("Id;\n\n");
                }
            } else {
                imports[0] = addJavaTypeImport(imports[0], value.getType());
                fields.append("\tprivate ").append(value.getType().javaType)
                        .append(" ").append(key).append(";\n\n");
            }
        });

        String content = template
                .replace("ENTITY_CLASS", entityClass)
                .replace("IMPORTS", imports[0])
                .replace("FIELDS", fields.toString());

        fileUtils.writeFile(project + srcPath + "/dto/" + entityClass + "Request.java", content);
    }

    private void writeMapperFile(Entity entity, Path project) throws IOException {
        String template = fileUtils.getTemplateContent(srcPath + "/mapper/MapperBase.java");
        String entityClass = StringUtils.capitalizeFirstCharacter(entity.getEntityName());
        String[] imports = {""};
        StringBuilder mapping = new StringBuilder("\t\tentity.setId(request.getId());\n");

        entity.getAttributes().forEach((key, value) -> {
            String property = StringUtils.capitalizeFirstCharacter(key);

            if (isLink(value.getType())) {
                String target = StringUtils.capitalizeFirstCharacter(value.getCustomType());
                String targetImport = importProjectBasePath + ".domain." + target + ";\n";
                if (!imports[0].contains(targetImport)) {
                    imports[0] += targetImport;
                }

                if (isList(value.getType())) {
                    imports[0] = addListImport(imports[0]);
                    imports[0] = addArrayListImport(imports[0]);
                    String pluralProperty = StringUtils.capitalizeFirstCharacter(pluralize(key));
                    String listVariable = StringUtils.lowerFirstCharacter(pluralProperty) + "Ref";
                    String itemVariable = StringUtils.lowerFirstCharacter(target) + "Ref";

                    mapping.append("\n\t\tif (request.get").append(pluralProperty).append("Ids() != null) {\n");
                    mapping.append("\t\t\tList<").append(target).append("> ").append(listVariable)
                            .append(" = new ArrayList<>();\n");
                    mapping.append("\t\t\tfor (Long id : request.get").append(pluralProperty)
                            .append("Ids()) {\n");
                    mapping.append("\t\t\t\tif (id != null) {\n");
                    mapping.append("\t\t\t\t\t").append(target).append(" ").append(itemVariable)
                            .append(" = new ").append(target).append("();\n");
                    mapping.append("\t\t\t\t\t").append(itemVariable).append(".setId(id);\n");
                    mapping.append("\t\t\t\t\t").append(listVariable).append(".add(")
                            .append(itemVariable).append(");\n");
                    mapping.append("\t\t\t\t}\n");
                    mapping.append("\t\t\t}\n");
                    mapping.append("\t\t\tentity.set").append(pluralProperty).append("(")
                            .append(listVariable).append(");\n");
                    mapping.append("\t\t}\n");
                } else {
                    String relationVariable = StringUtils.lowerFirstCharacter(property) + "Ref";

                    mapping.append("\n\t\tif (request.get").append(property).append("Id() != null) {\n");
                    mapping.append("\t\t\t").append(target).append(" ").append(relationVariable)
                            .append(" = new ").append(target).append("();\n");
                    mapping.append("\t\t\t").append(relationVariable).append(".setId(request.get")
                            .append(property).append("Id());\n");
                    mapping.append("\t\t\tentity.set").append(property).append("(")
                            .append(relationVariable).append(");\n");
                    mapping.append("\t\t}\n");
                }
            } else {
                mapping.append("\t\tentity.set").append(property).append("(request.get")
                        .append(property).append("());\n");
            }
        });

        String content = template
                .replace("ENTITY_CLASS", entityClass)
                .replace("IMPORTS", imports[0])
                .replace("MAPPING", mapping.toString());

        fileUtils.writeFile(project + srcPath + "/mapper/" + entityClass + "Mapper.java", content);
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

        String entityClass = StringUtils.capitalizeFirstCharacter(entity.getEntityName());
        String imports = importProjectBasePath + ".domain." + entityClass + ";\n";
        imports += importProjectBasePath + ".service." + entityClass + "Service;\n";

        String requestType = entityClass;
        String saveBody;
        String editBody;

        if (database.isDocument()) {
            saveBody = "return ResponseEntity.ok(SERVICE.save(request));";
            editBody = "return ResponseEntity.ok(SERVICE.edit(request));";
        } else {
            imports += importProjectBasePath + ".dto." + entityClass + "Request;\n";
            imports += importProjectBasePath + ".mapper." + entityClass + "Mapper;\n";
            requestType = entityClass + "Request";
            saveBody = "request.setId(null);\n        return ResponseEntity.ok(SERVICE.save("
                    + entityClass + "Mapper.toEntity(request)));";
            editBody = "return ResponseEntity.ok(SERVICE.edit(" + entityClass + "Mapper.toEntity(request)));";
        }

        String newControllerFileContent = baseControllerFileContent.replace("ENTITY_DECLARATION", entityClass);
        newControllerFileContent = newControllerFileContent.replace("IMPORTS", imports);
        newControllerFileContent = newControllerFileContent.replace("REQUEST_TYPE", requestType);
        newControllerFileContent = newControllerFileContent.replace("SAVE_BODY", saveBody);
        newControllerFileContent = newControllerFileContent.replace("EDIT_BODY", editBody);
        newControllerFileContent = newControllerFileContent.replace("SERVICE_DECLARATION", entityClass + "Service " + entity.getEntityName() + "Service;\n");
        newControllerFileContent = newControllerFileContent.replace("SERVICE", entity.getEntityName() + "Service");
        newControllerFileContent = newControllerFileContent.replace("REQUEST_MAPPING", "/api/" + entity.getEntityName());

        fileUtils.writeFile(project + srcPath + "/controller/" + entityClass + "Controller.java", newControllerFileContent);
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

        String inputType = entityClassName;
        String createBody;
        String updateBody;
        String idExpression = database.isDocument() ? "id" : "Long.valueOf(id)";
        String serviceVariable = entity.getEntityName() + "Service";

        if (database.isDocument()) {
            createBody = "input.setId(null);\n        return " + serviceVariable + ".save(input);";
            updateBody = "return " + serviceVariable + ".edit(input);";
        } else {
            imports += importProjectBasePath + ".dto." + entityClassName + "Request;\n";
            imports += importProjectBasePath + ".mapper." + entityClassName + "Mapper;\n";
            inputType = entityClassName + "Request";
            createBody = "input.setId(null);\n        return " + serviceVariable + ".save("
                    + entityClassName + "Mapper.toEntity(input));";
            updateBody = "return " + serviceVariable + ".edit(" + entityClassName + "Mapper.toEntity(input));";
        }

        String newControllerFileContent = baseControllerFileContent.replace("ENTITY_DECLARATION", entityClassName);
        newControllerFileContent = newControllerFileContent.replace("ENTITY", entity.getEntityName());
        newControllerFileContent = newControllerFileContent.replace("IMPORTS", imports);
        newControllerFileContent = newControllerFileContent.replace("INPUT_TYPE", inputType);
        newControllerFileContent = newControllerFileContent.replace("CREATE_BODY", createBody);
        newControllerFileContent = newControllerFileContent.replace("UPDATE_BODY", updateBody);
        newControllerFileContent = newControllerFileContent.replace("ID_EXPRESSION", idExpression);

        fileUtils.writeFile(project + srcPath + "/controller/" + entityClassName + "GraphQLController.java",
                newControllerFileContent);
    }

    private void writeGraphQlSchema(Domain domain, DatabaseEnum database, Path project) throws IOException {
        StringBuilder schema = new StringBuilder();
        schema.append("# Esquema GraphQL generado por Spring CRUD Initializr\n\n");
        schema.append("scalar Date\n");
        schema.append("scalar DateTime\n");
        schema.append("scalar Time\n");
        schema.append("scalar BigDecimal\n");
        schema.append("scalar UUID\n\n");

        for (Entity entity : domain.getEntities()) {
            schema.append(renderGraphQlType(entity)).append("\n");
            schema.append(renderGraphQlInput(entity, database)).append("\n");
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

    private String renderGraphQlInput(Entity entity, DatabaseEnum database) {
        String entityClassName = StringUtils.capitalizeFirstCharacter(entity.getEntityName());
        StringBuilder input = new StringBuilder("input ").append(entityClassName).append("Input {\n");
        if (!entity.isEmbedded()) {
            input.append("  id: ID\n");
        }
        for (Map.Entry<String, Attribute> entry : entity.getAttributes().entrySet()) {
            Attribute attribute = entry.getValue();
            String fieldName = graphQlFieldName(entry.getKey(), attribute.getType());

            if (!database.isDocument() && isLink(attribute.getType())) {
                String relationField = isList(attribute.getType())
                        ? fieldName + "Ids"
                        : fieldName + "Id";
                input.append("  ").append(relationField).append(": ")
                        .append(isList(attribute.getType()) ? "[ID]" : "ID").append("\n");
                continue;
            }

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

    private String addArrayListImport(String imports) {
        if (imports.contains("import java.util.ArrayList;")) {
            return imports;
        }
        return imports + "import java.util.ArrayList;\n";
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

    private void writeBuilderFile(Entity entity, Domain domain, Path project) throws IOException {
        String builderFile = fileUtils.getTemplateContent(testPath + "/builder/BuilderBase.java");
        String[] genericMethods = {""};
        String[] genericBuild = {""};
        String[] relationBuild = {""};
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
                String relatedBuilder = customType + "Builder";
                String backReferences = backReferenceCalls(domain, entity, customType);
                String relatedValue = relatedBuilder + ".generic(depth - 1)" + backReferences + ".build()";
                if (isList(typeEnum)) {
                    relatedValue = "new ArrayList<>(List.of(" + relatedValue + "))";
                    importString[0] = addListImport(importString[0]);
                    importString[0] = addArrayListImport(importString[0]);
                } else if (backReferences.contains("ArrayList")) {
                    importString[0] = addListImport(importString[0]);
                    importString[0] = addArrayListImport(importString[0]);
                }
                relationBuild[0] += "builder.with" + propertyName + "(" + relatedValue + ");\n\t\t";
            } else {
                importString[0] = addJavaTypeImport(importString[0], typeEnum);
                genericBuild[0] += "builder.with" + propertyName + "(" + defaultValue(value) + ");\n\t";
            }
            genericMethods[0] += "public " + domainEntity + "Builder with" + propertyName + "(" + type + " val) {\n\t";
            genericMethods[0] += "instance.set" + propertyName + "(val);\n\treturn this;\n}\n\n";
        });

        if (!relationBuild[0].isEmpty()) {
            genericBuild[0] += "if (depth > 0) {\n\t\t" + relationBuild[0] + "}\n\t";
        }

        int depth = longestSingleRelationChain(domain, entity) + 1;
        String newBuilderFile = builderFile.replace("DOMAIN", domainEntity);
        newBuilderFile = newBuilderFile.replace("GENERIC_DEPTH", String.valueOf(depth));
        newBuilderFile = newBuilderFile.replace("IMPORT", importString[0]);
        newBuilderFile = newBuilderFile.replace("GENERIC_BUILD", genericBuild[0]);
        newBuilderFile = newBuilderFile.replace("GENERIC_METHODS", genericMethods[0]);

        fileUtils.writeFile(project + testPath + "/builder/" + domainEntity + "Builder.java", newBuilderFile);
    }

    private String backReferenceCalls(Domain domain, Entity entity, String targetName) {
        Entity target = domain.getEntities().stream()
                .filter(candidate -> candidate.getEntityName().equalsIgnoreCase(targetName))
                .findFirst()
                .orElse(null);

        if (target == null) {
            return "";
        }

        StringBuilder calls = new StringBuilder();
        target.getAttributes().forEach((key, attribute) -> {
            if (!isLink(attribute.getType())
                    || attribute.getCustomType() == null
                    || !attribute.getCustomType().equalsIgnoreCase(entity.getEntityName())) {
                return;
            }

            String propertyName = isList(attribute.getType())
                    ? pluralize(StringUtils.capitalizeFirstCharacter(key))
                    : StringUtils.capitalizeFirstCharacter(key);

            if (isList(attribute.getType())) {
                calls.append(".with").append(propertyName)
                        .append("(new ArrayList<>(List.of(builder.instance)))");
            } else {
                calls.append(".with").append(propertyName).append("(builder.instance)");
            }
        });

        return calls.toString();
    }

    private int longestSingleRelationChain(Domain domain, Entity entity) {
        return longestSingleRelationChain(domain, entity, new HashSet<>());
    }

    private int longestSingleRelationChain(Domain domain, Entity entity, Set<String> visiting) {
        String entityKey = entity.getEntityName().toLowerCase();
        if (!visiting.add(entityKey)) {
            return 0;
        }

        int longest = 0;
        for (Map.Entry<String, Attribute> entry : entity.getAttributes().entrySet()) {
            Attribute attribute = entry.getValue();
            if (attribute.getType() != TypeEnum.CUSTOM
                    && attribute.getType() != TypeEnum.CUSTOM_ONE_TO_ONE) {
                continue;
            }

            Entity target = domain.getEntities().stream()
                    .filter(candidate -> candidate.getEntityName()
                            .equalsIgnoreCase(attribute.getCustomType()))
                    .findFirst()
                    .orElse(null);

            if (target != null) {
                longest = Math.max(longest, 1 + longestSingleRelationChain(domain, target, visiting));
            }
        }

        visiting.remove(entityKey);
        return longest;
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
