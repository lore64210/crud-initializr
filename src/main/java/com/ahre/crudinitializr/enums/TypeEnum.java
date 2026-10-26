package com.ahre.crudinitializr.enums;

public enum TypeEnum {
    STRING("String"),
    INTEGER("Integer"),
    LONG("Long"),
    BOOLEAN("Boolean"),
    DATE("LocalDate"),
    DATETIME("LocalDateTime"),
    TIME("LocalTime"),
    DECIMAL("BigDecimal"),
    DOUBLE("Double"),
    UUID("UUID"),
    CUSTOM(null),
    CUSTOM_LIST(null),
    CUSTOM_ONE_TO_ONE(null),
    CUSTOM_MANY_TO_MANY(null),
    COMPOSITION(null),
    COMPOSITION_LIST(null);

    public final String javaType;

    private TypeEnum(String javaType) {
        this.javaType = javaType;
    }

    public boolean isScalar() {
        return javaType != null;
    }
}
