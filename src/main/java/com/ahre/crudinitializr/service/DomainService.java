package com.ahre.crudinitializr.service;

import com.ahre.crudinitializr.enums.TypeEnum;
import com.ahre.crudinitializr.enums.ValidationEnum;
import com.ahre.crudinitializr.utils.FileUtils;
import com.ahre.crudinitializr.utils.StringUtils;
import com.ahre.crudinitializr.vo.Domain;
import com.ahre.crudinitializr.vo.Entity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class DomainService {

    private final FileUtils fileUtils;
    
    @Value("${staticPath}")
    private String staticPath;
    
    @Value("${srcPath}")
    private String srcPath; 
    
    @Value("${testPath}")
    private String testPath;
    
    @Value("${importProjectBasePath}")
    private String importProjectBasePath;

    public Path processDomain(Domain domain) {
        try {
            validateDomain(domain);
            Path project = fileUtils.createOutputFolder();
            for (Entity entity : domain.getEntities()) {
                writeDomainFile(entity, project);
                writeServiceFile(entity, project);
                writeRepositoryFile(entity, project);
                writeControllerFile(entity, project);
                writeBuilderFile(entity, project);
                writeTestFile(entity, project);
            }
            writeSchemaSQLFiles(domain, project);
            fileUtils.deleteDirectory(Paths.get(project + srcPath + "/domain/DomainBase.java"));
            fileUtils.deleteDirectory(Paths.get(project + srcPath + "/service/ServiceBase.java"));
            fileUtils.deleteDirectory(Paths.get(project + srcPath + "/repository/RepositoryBase.java"));
            fileUtils.deleteDirectory(Paths.get(project + srcPath + "/controller/ControllerBase.java"));
            fileUtils.deleteDirectory(Paths.get(project + testPath + "/builder/BuilderBase.java"));
            fileUtils.deleteDirectory(Paths.get(project + testPath + "/tests/service/ServiceTestBase.java"));
            return project;
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo crear el proyecto", e);
        }
    }

    private void validateDomain(Domain domain) {
        Assert.notNull(domain.getTitle(), "El nombre del proyecto no puede estar vacio");
        Assert.isTrue(domain.getTitle().length() != 0, "El nombre del proyecto no puede estar vacío");
        Assert.isTrue(domain.getTitle().length() <= 30, "El nombre del proyecto no puede tener mas de 30 caracteres");
        domain.getEntities().forEach(e -> {
            Assert.notNull(e.getEntityName(), "El nombre de una entidad no puede estar vacía");
            Assert.isTrue(e.getEntityName().length() != 0, "El nombre de una entidad no puede estar vacía");
            Assert.isTrue(e.getEntityName().length() <= 30, "El nombre de una entidad no puede tener mas de 30 caracteres");
            e.getAttributes().forEach((key, value) -> {
                Assert.notNull(value.getType(), "Todos los atributos deben tener un tipo");
                Assert.notNull(key, "Todos los atributos deben tener un nombre");
            });
        });
    }

    private void writeDomainFile(Entity entity, Path project) throws IOException {
        String baseDomainFileContent = fileUtils.getContentFromFile(staticPath + srcPath + "/domain/DomainBase.java");
        String newDomainFileContent = "";
        final String[] domainAttributes = {""};
        final String[] imports = {""};
        entity.getAttributes().forEach((key, value) -> {
            if (value.getType().name().equals("CUSTOM")) {
                domainAttributes[0] += "\n\t@ManyToOne(cascade = CascadeType.ALL)\n";
                domainAttributes[0] += "\tprivate " + StringUtils.capitalizeFirstCharacter(key) + " " + key + ";\n";
            } else if (value.getType().name().equals("CUSTOM_LIST")) {
                domainAttributes[0] += "\n\t@OneToMany(cascade = CascadeType.ALL)\n";
                domainAttributes[0] += "\t@JoinColumn(name = \"" + StringUtils.transformToSnakeCase(key) + "_id\")\n";
                domainAttributes[0] += "\tprivate List<" + StringUtils.capitalizeFirstCharacter(key) + "> " + key + "s;\n";
                imports[0] += importProjectBasePath + ".domain." + StringUtils.capitalizeFirstCharacter(key) + ";\n";
                if (!imports[0].contains("import javax.persistence.*;")) {
                    imports[0] += "import javax.persistence.*;\n";
                }
                if (!imports[0].contains("import java.util.ArrayList;\nimport java.util.List;\n")) {
                    imports[0] += "import java.util.ArrayList;\nimport java.util.List;\n";
                }
            } else {
                domainAttributes[0] += "\n\tprivate " + StringUtils.capitalizeFirstCharacter(value.getType().name().toLowerCase()) + " " + key + ";\n";
            }
        });

        newDomainFileContent = baseDomainFileContent.replace("TABLE_NAME", StringUtils.transformToSnakeCase(entity.getEntityName()));
        newDomainFileContent = newDomainFileContent.replace("DOMAIN_NAME", StringUtils.capitalizeFirstCharacter(entity.getEntityName()));
        newDomainFileContent = newDomainFileContent.replace("DOMAIN_ATTRIBUTES", domainAttributes[0]);
        newDomainFileContent = newDomainFileContent.replace("IMPORTS", imports[0]);

        fileUtils.writeFile(project + srcPath + "/domain/" + StringUtils.capitalizeFirstCharacter(entity.getEntityName()) +  ".java", newDomainFileContent);
    }

    private void writeServiceFile(Entity entity, Path project) throws IOException {
        String baseServiceFileContent = fileUtils.getContentFromFile(staticPath + srcPath + "/service/serviceBase.java");
        String newServiceFileContent = "";
        final String[] saveExternalEntities = {""};
        final String[] externalEntitiesServices = {""};
        final String[] validations = {""};

        entity.getAttributes().forEach((key, value) -> {
            if (value.getType().name().equals("CUSTOM")) {
                saveExternalEntities[0] += "\n\t\t" + key + "Service.save("+ entity.getEntityName() + ".get" + StringUtils.capitalizeFirstCharacter(key) +"());\n";
                externalEntitiesServices[0] += "\n\tprivate final " + StringUtils.capitalizeFirstCharacter(key) + "Service " + key + "Service;\n";
            }

            if (!value.getValidations().isEmpty()) {
                value.getValidations().forEach(validation -> {
                    if (validation.equals(ValidationEnum.NOT_BLANK)) {
                        validations[0] += "\n\tAssert.isTrue(!" + entity.getEntityName() + ".get" + StringUtils.capitalizeFirstCharacter(key) + (value.getType().equals(TypeEnum.CUSTOM_LIST) ? "s" : "") + "().isEmpty(), \""+ key + " can not be "+ validation.message + "\");\n";
                    } else {
                        validations[0] += "\n\tAssert." + validation.validation + "(" + entity.getEntityName() + ".get" + StringUtils.capitalizeFirstCharacter(key) + (value.getType().equals(TypeEnum.CUSTOM_LIST) ? "s" : "") + "(), \""+ key + " can not be "+ validation.message + "\");\n";
                    }
                });
            }
        });

        newServiceFileContent = baseServiceFileContent.replace("SERVICE_DECLARATION", StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + "Service");
        newServiceFileContent = newServiceFileContent.replace("REPOSITORY_DECLARATION", StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + "Repository");
        newServiceFileContent = newServiceFileContent.replace("REPOSITORY", entity.getEntityName() + "Repository");
        newServiceFileContent = newServiceFileContent.replace("DOMAIN_DECLARATION", StringUtils.capitalizeFirstCharacter(entity.getEntityName()));
        newServiceFileContent = newServiceFileContent.replace("DOMAIN", entity.getEntityName());
        newServiceFileContent = newServiceFileContent.replace("SAVE_EXTERNAL_ENTITIES", saveExternalEntities[0]);
        newServiceFileContent = newServiceFileContent.replace("EXTERNAL_ENTITIES_SERVICES", externalEntitiesServices[0]);
        newServiceFileContent = newServiceFileContent.replace("VALIDATIONS", validations[0]);

        fileUtils.writeFile(project + srcPath + "/service/" + StringUtils.capitalizeFirstCharacter(entity.getEntityName()) +  "Service.java", newServiceFileContent);
    }

    private void writeRepositoryFile(Entity entity, Path project) throws IOException {
        String baseRepositoryFileContent = fileUtils.getContentFromFile(staticPath + srcPath + "/repository/RepositoryBase.java");
        String newRepositoryFileContent = "";

        newRepositoryFileContent = baseRepositoryFileContent.replace("ENTITY", StringUtils.capitalizeFirstCharacter(entity.getEntityName()));
        newRepositoryFileContent = newRepositoryFileContent.replace("IMPORT", importProjectBasePath + ".domain." + StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + ";\n");

        fileUtils.writeFile(project + srcPath + "/repository/" + StringUtils.capitalizeFirstCharacter(entity.getEntityName()) +  "Repository.java", newRepositoryFileContent);
    }

    private void writeControllerFile(Entity entity, Path project) throws IOException {
        String baseControllerFileContent = fileUtils.getContentFromFile(staticPath + srcPath + "/controller/ControllerBase.java");
        String newControllerFileContent = "";

        String imports = importProjectBasePath + ".domain." + StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + ";\n";
        imports += importProjectBasePath + ".service." + StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + "Service;\n";

        newControllerFileContent = baseControllerFileContent.replace("ENTITY_DECLARATION", StringUtils.capitalizeFirstCharacter(entity.getEntityName()));
        newControllerFileContent = newControllerFileContent.replace("ENTITY", entity.getEntityName());
        newControllerFileContent = newControllerFileContent.replace("IMPORTS", imports);
        newControllerFileContent = newControllerFileContent.replace("SERVICE_DECLARATION", StringUtils.capitalizeFirstCharacter(entity.getEntityName()) + "Service " + entity.getEntityName() + "Service;\n");
        newControllerFileContent = newControllerFileContent.replace("SERVICE", entity.getEntityName() + "Service");
        newControllerFileContent = newControllerFileContent.replace("REQUEST_MAPPING", "/api/" + entity.getEntityName());

        fileUtils.writeFile(project + srcPath + "/controller/" + StringUtils.capitalizeFirstCharacter(entity.getEntityName()) +  "Controller.java", newControllerFileContent);
    }

    private void writeSchemaSQLFiles(Domain domain, Path project) throws IOException {
        String sqlFileContent = fileUtils.getContentFromFile(staticPath + "/doc/scripts/schema.sql");
        String sqlTestFileContent = fileUtils.getContentFromFile(staticPath + "/test/resources/schema.sql");
        String newSqlFileContent = "";
        String newSqlTestFileContent = "";

        String[] dropTables = {""};
        String[] createTables = {""};

        domain.getEntities().forEach(entity -> {
            String entityName = StringUtils.transformToSnakeCase(entity.getEntityName());
            dropTables[0] += "DROP TABLE IF EXISTS "+ entityName +";\n";
            createTables[0] += "CREATE TABLE IF NOT EXISTS "+ entityName +"(\n\tid BIGINT AUTO_INCREMENT PRIMARY KEY,\n\tfecha_creacion DATETIME,\n\t";
            entity.getAttributes().forEach((key, value) -> {
                String sqlKey = StringUtils.transformToSnakeCase(key);
                if (value.getType().equals(TypeEnum.CUSTOM)) {
                    createTables[0] += sqlKey + "_id BIGINT,\n\t";
                    createTables[0] += "CONSTRAINT fk_" + sqlKey + " FOREIGN KEY ("+ sqlKey +"_id) REFERENCES " + sqlKey + "(id),\n\t";
                } else if (!value.getType().equals(TypeEnum.CUSTOM_LIST)){
                    createTables[0] += sqlKey + " " + value.getType().sqlType + ",\n\t";
                }
            });
            domain.getEntities().forEach(e -> e.getAttributes().forEach((key, value) -> {
                if (key.equals(entity.getEntityName()) && value.getType().equals(TypeEnum.CUSTOM_LIST)) {
                    String foreignKey = StringUtils.transformToSnakeCase(key);
                    String entityReferenced = StringUtils.transformToSnakeCase(e.getEntityName());
                    createTables[0] += entityReferenced + "_id BIGINT,\n\t";
                    createTables[0] += "CONSTRAINT fk_" + foreignKey + "_" + entityReferenced + " FOREIGN KEY ("+ entityReferenced +"_id) REFERENCES " + entityReferenced + "(id),\n\t";
                }
            }));

            createTables[0] = StringUtils.replaceLast(createTables[0], ",", "");
            createTables[0] += ");\n\n";
        });

        newSqlFileContent = sqlFileContent.replace("CREATE_TABLES", createTables[0]);
        newSqlTestFileContent = sqlTestFileContent.replace("CREATE_TABLES", createTables[0]);
        newSqlTestFileContent = newSqlTestFileContent.replace("DROP_TABLES", dropTables[0]);

        fileUtils.writeFile(project + "/doc/scripts/schema.sql", newSqlFileContent);
        fileUtils.writeFile(project + "/test/resources/schema.sql", newSqlTestFileContent);
    }

    private void writeBuilderFile(Entity entity, Path project) throws IOException {
        String builderFile = fileUtils.getContentFromFile(staticPath + testPath + "/builder/BuilderBase.java");
        String newBuilderFile = "";
        String[] genericMethods = {""};
        String[] genericBuild = {""};
        String domainEntity = StringUtils.capitalizeFirstCharacter(entity.getEntityName());
        String[] importString = {importProjectBasePath + ".domain." + domainEntity + ";\n"};

        entity.getAttributes().forEach((key, value) -> {
            String attribute = StringUtils.capitalizeFirstCharacter(key);
            String type = StringUtils.capitalizeFirstCharacter(value.getType().name());
            if (value.getType().equals(TypeEnum.CUSTOM)) {
                type = attribute;
                importString[0] += importProjectBasePath + ".domain." + attribute + ";\n";
            } else if (value.getType().equals(TypeEnum.CUSTOM_LIST)) {
                type = "List<" + attribute + ">";
                importString[0] += importProjectBasePath + ".domain." + attribute + ";\n";
                if (!importString[0].contains("List")) {
                    importString[0] += "import java.util.List;\n";

                }
            }
            boolean isCustom = value.getType().equals(TypeEnum.CUSTOM_LIST) || value.getType().equals(TypeEnum.CUSTOM);
            String s = (value.getType().equals(TypeEnum.CUSTOM_LIST) ? "s" : "");
            genericMethods[0] += "public " + domainEntity + "Builder with" + attribute + s + "(" + (isCustom ? type : StringUtils.capitalizeFirstCharacter(type.toLowerCase())) + " val) {\n\t";
            genericMethods[0] += "instance.set" + attribute + s + "(val);\n\treturn this;\n}\n\n";

            genericBuild[0] += "builder.with" + attribute + s + "(null);\n\t";
        });

        newBuilderFile = builderFile.replace("DOMAIN", domainEntity);
        newBuilderFile = newBuilderFile.replace("IMPORT", importString[0]);
        newBuilderFile = newBuilderFile.replace("GENERIC_BUILD", genericBuild[0]);
        newBuilderFile = newBuilderFile.replace("GENERIC_METHODS", genericMethods[0]);

        fileUtils.writeFile(project + testPath + "/builder/" + domainEntity + "Builder.java", newBuilderFile);
    }

    private void writeTestFile(Entity entity, Path project) throws IOException {
        String domainEntity = StringUtils.capitalizeFirstCharacter(entity.getEntityName());
        String testFile = fileUtils.getContentFromFile(staticPath + testPath + "/tests/service/ServiceTestBase.java");
        String newTestFile = "";
        String imports = importProjectBasePath + ".service" + domainEntity + "Service;\n";
        imports += importProjectBasePath + ".domain" + domainEntity + ";\n";

        newTestFile = testFile.replace("DOMAIN_DECLARATION", domainEntity);
        newTestFile = newTestFile.replace("DOMAIN_DATABASE", StringUtils.transformToSnakeCase(entity.getEntityName()));
        newTestFile = newTestFile.replace("DOMAIN", entity.getEntityName());
        newTestFile = newTestFile.replace("IMPORTS", imports);
        newTestFile = newTestFile.replace("SERVICE_DECLARATION", domainEntity + "Service");
        newTestFile = newTestFile.replace("SERVICE", entity.getEntityName() + "Service");

        fileUtils.writeFile(project + testPath + "/tests/service/" + domainEntity + "ServiceTest.java", newTestFile);
    }
}
