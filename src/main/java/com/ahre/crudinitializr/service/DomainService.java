package com.ahre.crudinitializr.service;

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
import java.nio.file.Path;
import java.util.ArrayList;
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
        Path project = null;
        try {
            project = fileUtils.createOutputFolder();
            writeStaticFiles(project);
            writeProjectReadme(domain.getTitle(), database, project);
            writeDockerCompose(domain.getTitle(), database, project);
            writePomFile(domain.getTitle(), database, project);
            writeApplicationProperties(database, project);
            for (Entity entity : domain.getEntities()) {
                writeDomainFile(entity, project);
                writeServiceFile(entity, project);
                writeRepositoryFile(entity, project);
                writeControllerFile(entity, project);
                writeBuilderFile(entity, project);
                writeTestFile(entity, project);
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

                if (type == TypeEnum.CUSTOM || type == TypeEnum.CUSTOM_LIST) {
                    String customType = value.getCustomType();
                    Assert.isTrue(customType != null && !customType.trim().isEmpty(), "El tipo CUSTOM del atributo '" + key + "' no puede estar vacío");
                    Assert.isTrue(isJavaIdentifier(customType), "El tipo CUSTOM '" + customType + "' del atributo '" + key + "' no es válido");
                }

                List<ValidationEnum> validations = value.getValidations();
                if (validations != null) {
                    for (ValidationEnum validation : validations) {
                        if (validation == null) {
                            continue;
                        }
                        boolean allowed = validation == ValidationEnum.NOT_NULL
                                || (validation == ValidationEnum.NOT_BLANK && type == TypeEnum.STRING)
                                || (validation == ValidationEnum.NOT_EMPTY && (type == TypeEnum.STRING || type == TypeEnum.CUSTOM_LIST));
                        Assert.isTrue(allowed, "La validación " + validation + " no es válida para el atributo '" + key + "' de tipo " + type);
                    }
                }
            }
        }

        for (Entity entity : entities) {
            String entityClassName = StringUtils.capitalizeFirstCharacter(entity.getEntityName());
            for (Map.Entry<String, Attribute> entry : entity.getAttributes().entrySet()) {
                Attribute value = entry.getValue();
                if (value.getType() == TypeEnum.CUSTOM || value.getType() == TypeEnum.CUSTOM_LIST) {
                    String customClassName = StringUtils.capitalizeFirstCharacter(value.getCustomType());
                    Assert.isTrue(entityClassNames.contains(customClassName),
                            "El atributo '" + entry.getKey() + "' de la entidad '" + entity.getEntityName() + "' referencia a una entidad inexistente: " + value.getCustomType());
                    Assert.isTrue(!customClassName.equals(entityClassName),
                            "El atributo '" + entry.getKey() + "' de la entidad '" + entity.getEntityName() + "' no puede referenciarse a sí mismo");
                }
            }
        }
    }

    private void writeStaticFiles(Path project) throws IOException {
        List<String> templates = List.of(
                srcPath + "/ProjectBaseApplication.java",
                srcPath + "/controller/advice/AdviceController.java",
                testPath + "/tests/ApplicationTests.java",
                "/src/test/resources/application.properties"
        );
        for (String template : templates) {
            fileUtils.copyTemplate(template, project);
        }
        fileUtils.copyTemplate("/gitignore.template", project, ".gitignore");
    }

    private void writePomFile(String title, DatabaseEnum database, Path project) throws IOException {
        String content = fileUtils.getTemplateContent("/pom.xml");
        String escapedTitle = StringUtils.escapeXml(title.trim());
        content = content.replace("PROJECT_ARTIFACT_ID", StringUtils.toArtifactId(title));
        content = content.replace("PROJECT_NAME", escapedTitle);
        content = content.replace("PROJECT_DESCRIPTION", escapedTitle);
        content = content.replace("DATABASE_LABEL", database.getLabel());
        content = content.replace("DATABASE_DEPENDENCY", renderDatabaseDependencies(database));
        content = content.replace("TEST_H2_DEPENDENCY",
                database.isEmbedded() ? "" : renderDependency("com.h2database", "h2", "test"));
        fileUtils.writeFile(project + "/pom.xml", content);
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

    private String renderDependency(String groupId, String artifactId, String scope) {
        return "\t\t<dependency>\n"
                + "\t\t\t<groupId>" + groupId + "</groupId>\n"
                + "\t\t\t<artifactId>" + artifactId + "</artifactId>\n"
                + "\t\t\t<scope>" + scope + "</scope>\n"
                + "\t\t</dependency>";
    }

    private void writeApplicationProperties(DatabaseEnum database, Path project) throws IOException {
        String template = fileUtils.getTemplateContent("/src/main/resources/application.properties");
        String content = template
                .replace("DATABASE_LABEL", database.getLabel())
                .replace("DATABASE_URL", "${DB_URL:" + database.getJdbcUrl() + "}")
                .replace("DATABASE_USERNAME", "${DB_USER:" + database.getUsername() + "}")
                .replace("DATABASE_PASSWORD", "${DB_PASSWORD:" + database.getPassword() + "}")
                .replace("DATABASE_EXTRA_PROPERTIES", extraProperties(database));
        fileUtils.writeFile(project + "/src/main/resources/application.properties", content);
    }

    private String extraProperties(DatabaseEnum database) {
        return switch (database) {
            case H2 -> "spring.h2.console.enabled=true\nspring.h2.console.path=/h2-console\n";
            case SQLITE -> "spring.datasource.driver-class-name=org.sqlite.JDBC\n"
                    + "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect\n";
            case MYSQL, MARIADB, POSTGRESQL -> "";
        };
    }

    private void writeProjectReadme(String title, DatabaseEnum database, Path project) throws IOException {
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
                .replace("DATABASE_RUN_INSTRUCTIONS", runInstructions);
        fileUtils.writeFile(project + "/README.md", content);
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

    private void writeDomainFile(Entity entity, Path project) throws IOException {
        String baseDomainFileContent = fileUtils.getTemplateContent(srcPath + "/domain/DomainBase.java");
        final String[] domainAttributes = {""};
        final String[] imports = {""};
        entity.getAttributes().forEach((key, value) -> {
            if (value.getType() == TypeEnum.CUSTOM) {
                domainAttributes[0] += "\n\t@ManyToOne(cascade = CascadeType.ALL)\n";
                domainAttributes[0] += "\tprivate " + StringUtils.capitalizeFirstCharacter(value.getCustomType()) + " " + key + ";\n";
            } else if (value.getType() == TypeEnum.CUSTOM_LIST) {
                domainAttributes[0] += "\n\t@OneToMany(cascade = CascadeType.ALL)\n";
                String joinColumn = StringUtils.transformToSnakeCase(entity.getEntityName()) + "_"
                        + StringUtils.transformToSnakeCase(key) + "_id";
                domainAttributes[0] += "\t@JoinColumn(name = \"" + joinColumn + "\")\n";
                domainAttributes[0] += "\tprivate List<" + StringUtils.capitalizeFirstCharacter(value.getCustomType()) + "> " + key + "s;\n";
                if (!imports[0].contains("import java.util.List;")) {
                    imports[0] += "import java.util.List;\n";
                }
            } else {
                domainAttributes[0] += "\n\tprivate " + StringUtils.capitalizeFirstCharacter(value.getType().name().toLowerCase()) + " " + key + ";\n";
            }
        });

        String newDomainFileContent = baseDomainFileContent.replace("TABLE_NAME", StringUtils.transformToSnakeCase(entity.getEntityName()));
        newDomainFileContent = newDomainFileContent.replace("DOMAIN_NAME", StringUtils.capitalizeFirstCharacter(entity.getEntityName()));
        newDomainFileContent = newDomainFileContent.replace("DOMAIN_ATTRIBUTES", domainAttributes[0]);
        newDomainFileContent = newDomainFileContent.replace("IMPORTS", imports[0]);

        fileUtils.writeFile(project + srcPath + "/domain/" + StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + ".java", newDomainFileContent);
    }

    private void writeServiceFile(Entity entity, Path project) throws IOException {
        String baseServiceFileContent = fileUtils.getTemplateContent(srcPath + "/service/ServiceBase.java");
        final String[] saveExternalEntities = {""};
        final String[] externalEntitiesServices = {""};
        final String[] validations = {""};
        final Set<String> externalServices = new HashSet<>();

        entity.getAttributes().forEach((key, value) -> {
            String getter = entity.getEntityName() + ".get" + StringUtils.capitalizeFirstCharacter(key)
                    + (value.getType() == TypeEnum.CUSTOM_LIST ? "s" : "") + "()";

            if (value.getType() == TypeEnum.CUSTOM) {
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
                    if (validation == ValidationEnum.NOT_BLANK) {
                        validations[0] += "\n\tAssert.isTrue(" + getter + " != null && !" + getter + ".isBlank(), \"" + key + " can not be " + validation.message + "\");\n";
                    } else {
                        validations[0] += "\n\tAssert." + validation.validation + "(" + getter + ", \"" + key + " can not be " + validation.message + "\");\n";
                    }
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

    private void writeRepositoryFile(Entity entity, Path project) throws IOException {
        String baseRepositoryFileContent = fileUtils.getTemplateContent(srcPath + "/repository/RepositoryBase.java");

        String newRepositoryFileContent = baseRepositoryFileContent.replace("ENTITY", StringUtils.capitalizeFirstCharacter(entity.getEntityName()));
        newRepositoryFileContent = newRepositoryFileContent.replace("IMPORT", importProjectBasePath + ".domain." + StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + ";\n");

        fileUtils.writeFile(project + srcPath + "/repository/" + StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + "Repository.java", newRepositoryFileContent);
    }

    private void writeControllerFile(Entity entity, Path project) throws IOException {
        String baseControllerFileContent = fileUtils.getTemplateContent(srcPath + "/controller/ControllerBase.java");

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

    private void writeSchemaSQLFiles(Domain domain, DatabaseEnum database, Path project) throws IOException {
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
                } else if (value.getType() != TypeEnum.CUSTOM_LIST) {
                    table.columns.add(new ColumnDefinition(StringUtils.transformToSnakeCase(key), value.getType().sqlType));
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
                String type = column.identity ? dialect.identityColumn : column.type;
                columns.add("\t" + dialect.quote(column.name) + " " + type);
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

        private static SqlDialect from(DatabaseEnum database) {
            return switch (database) {
                case MYSQL -> MYSQL;
                case MARIADB -> MARIADB;
                case POSTGRESQL -> POSTGRESQL;
                case H2 -> H2;
                case SQLITE -> SQLITE;
            };
        }
    }

    private static final class TableDefinition {
        private final String name;
        private final List<ColumnDefinition> columns = new ArrayList<>();

        private TableDefinition(String name) {
            this.name = name;
        }
    }

    private static final class ColumnDefinition {
        private final String name;
        private final String type;
        private final boolean identity;

        private ColumnDefinition(String name, String type) {
            this(name, type, false);
        }

        private ColumnDefinition(String name, String type, boolean identity) {
            this.name = name;
            this.type = type;
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

    private String defaultValue(TypeEnum type) {
        return switch (type) {
            case STRING -> "\"test\"";
            case INTEGER -> "1";
            case LONG -> "1L";
            case BOOLEAN -> "true";
            default -> "null";
        };
    }

    private void writeBuilderFile(Entity entity, Path project) throws IOException {
        String builderFile = fileUtils.getTemplateContent(testPath + "/builder/BuilderBase.java");
        String[] genericMethods = {""};
        String[] genericBuild = {""};
        String domainEntity = StringUtils.capitalizeFirstCharacter(entity.getEntityName());
        String[] importString = {importProjectBasePath + ".domain." + domainEntity + ";\n"};

        entity.getAttributes().forEach((key, value) -> {
            String attribute = StringUtils.capitalizeFirstCharacter(key);
            String type = StringUtils.capitalizeFirstCharacter(value.getType().name());
            if (value.getType() == TypeEnum.CUSTOM) {
                type = StringUtils.capitalizeFirstCharacter(value.getCustomType());
                String customImport = importProjectBasePath + ".domain." + type + ";\n";
                if (!importString[0].contains(customImport)) {
                    importString[0] += customImport;
                }
            } else if (value.getType() == TypeEnum.CUSTOM_LIST) {
                String customImport = importProjectBasePath + ".domain." + StringUtils.capitalizeFirstCharacter(value.getCustomType()) + ";\n";
                type = "List<" + StringUtils.capitalizeFirstCharacter(value.getCustomType()) + ">";
                if (!importString[0].contains(customImport)) {
                    importString[0] += customImport;
                }
                if (!importString[0].contains("import java.util.List;")) {
                    importString[0] += "import java.util.List;\n";
                }
            }
            boolean isCustom = value.getType() == TypeEnum.CUSTOM_LIST || value.getType() == TypeEnum.CUSTOM;
            String s = (value.getType() == TypeEnum.CUSTOM_LIST ? "s" : "");
            genericMethods[0] += "public " + domainEntity + "Builder with" + attribute + s + "(" + (isCustom ? type : StringUtils.capitalizeFirstCharacter(type.toLowerCase())) + " val) {\n\t";
            genericMethods[0] += "instance.set" + attribute + s + "(val);\n\treturn this;\n}\n\n";
            genericBuild[0] += "builder.with" + attribute + s + "(" + defaultValue(value.getType()) + ");\n\t";
        });

        String newBuilderFile = builderFile.replace("DOMAIN", domainEntity);
        newBuilderFile = newBuilderFile.replace("IMPORT", importString[0]);
        newBuilderFile = newBuilderFile.replace("GENERIC_BUILD", genericBuild[0]);
        newBuilderFile = newBuilderFile.replace("GENERIC_METHODS", genericMethods[0]);

        fileUtils.writeFile(project + testPath + "/builder/" + domainEntity + "Builder.java", newBuilderFile);
    }

    private void writeTestFile(Entity entity, Path project) throws IOException {
        String domainEntity = StringUtils.capitalizeFirstCharacter(entity.getEntityName());
        String testFile = fileUtils.getTemplateContent(testPath + "/tests/service/ServiceTestBase.java");

        String newTestFile = testFile.replace("DOMAIN_DECLARATION", domainEntity);
        newTestFile = newTestFile.replace("DOMAIN", entity.getEntityName());
        newTestFile = newTestFile.replace("SERVICE_DECLARATION", domainEntity + "Service");
        newTestFile = newTestFile.replace("SERVICE", entity.getEntityName() + "Service");

        fileUtils.writeFile(project + testPath + "/tests/service/" + domainEntity + "ServiceTest.java", newTestFile);
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
