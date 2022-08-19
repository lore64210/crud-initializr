package com.ahre.crudinitializr.enums;

public enum ValidationEnum {
    NOT_NULL("notNull", "null"),
    NOT_BLANK("notBlank", "blank"),
    NOT_EMPTY("notEmpty", "empty");

    public final String validation;
    public final String message;

    private ValidationEnum(String validation, String message) {
        this.validation = validation;
        this.message = message;
    }
}
