package com.ahre.crudinitializr.enums;

public enum ValidationEnum {
    NOT_NULL("can not be null", false),
    NOT_BLANK("can not be blank", false),
    NOT_EMPTY("can not be empty", false),
    PAST("must be in the past", false),
    PAST_OR_PRESENT("must be in the past or present", false),
    FUTURE("must be in the future", false),
    FUTURE_OR_PRESENT("must be in the future or present", false),
    POSITIVE("must be positive", false),
    NEGATIVE("must be negative", false),
    POSITIVE_OR_ZERO("must be positive or zero", false),
    NEGATIVE_OR_ZERO("must be negative or zero", false),
    MIN("must be greater than or equal to", true),
    MAX("must be less than or equal to", true);

    public final String message;
    public final boolean requiresValue;

    ValidationEnum(String message, boolean requiresValue) {
        this.message = message;
        this.requiresValue = requiresValue;
    }
}
