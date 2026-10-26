package com.ahre.crudinitializr.enums;

import lombok.Getter;

@Getter
public enum DatabaseEnum {

    MYSQL(
            "MySQL",
            "com.mysql",
            "mysql-connector-j",
            "jdbc:mysql://localhost:3306/test?zeroDateTimeBehavior=convertToNull&serverTimezone=America/Buenos_Aires",
            "test",
            "test",
            "mysql:8.4",
            "mysql"
    ),
    MARIADB(
            "MariaDB",
            "org.mariadb.jdbc",
            "mariadb-java-client",
            "jdbc:mariadb://localhost:3306/test",
            "test",
            "test",
            "mariadb:11",
            "mariadb"
    ),
    POSTGRESQL(
            "PostgreSQL",
            "org.postgresql",
            "postgresql",
            "jdbc:postgresql://localhost:5432/test",
            "test",
            "test",
            "postgres:17",
            "postgresql"
    ),
    H2(
            "H2",
            "com.h2database",
            "h2",
            "jdbc:h2:file:./data/test;DB_CLOSE_ON_EXIT=FALSE",
            "sa",
            "",
            null,
            "h2"
    ),
    SQLITE(
            "SQLite",
            "org.xerial",
            "sqlite-jdbc",
            "jdbc:sqlite:./data/test.db?foreign_keys=on",
            "",
            "",
            null,
            "sqlite"
    ),
    MONGODB(
            "MongoDB",
            "org.springframework.boot",
            "spring-boot-starter-data-mongodb",
            "mongodb://localhost:27017/test",
            "",
            "",
            "mongo:7",
            "mongodb"
    );

    private final String label;
    private final String driverGroupId;
    private final String driverArtifactId;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String dockerImage;
    private final String key;

    DatabaseEnum(String label,
                 String driverGroupId,
                 String driverArtifactId,
                 String jdbcUrl,
                 String username,
                 String password,
                 String dockerImage,
                 String key) {
        this.label = label;
        this.driverGroupId = driverGroupId;
        this.driverArtifactId = driverArtifactId;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.dockerImage = dockerImage;
        this.key = key;
    }

    public boolean isEmbedded() {
        return this == H2;
    }

    public boolean isDocument() {
        return this == MONGODB;
    }

    public boolean hasSchema() {
        return !isDocument();
    }

    public boolean hasDockerImage() {
        return dockerImage != null;
    }
}
