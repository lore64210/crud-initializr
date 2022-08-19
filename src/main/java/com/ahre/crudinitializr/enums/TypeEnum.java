package com.ahre.crudinitializr.enums;

public enum TypeEnum {
    STRING("VARCHAR(255)"),
    INTEGER("INT"),
    LONG("BIGINT"),
    BOOLEAN("BOOLEAN"),
    CUSTOM("CUSTOM"),
    CUSTOM_LIST("CUSTOM_LIST");

    public final String sqlType;

    private TypeEnum(String sqlType) {
        this.sqlType = sqlType;
    }
}
