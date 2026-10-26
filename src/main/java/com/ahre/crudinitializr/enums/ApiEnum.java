package com.ahre.crudinitializr.enums;

import lombok.Getter;

@Getter
public enum ApiEnum {

    REST("REST"),
    GRAPHQL("GraphQL");

    private final String label;

    ApiEnum(String label) {
        this.label = label;
    }

    public boolean isGraphQL() {
        return this == GRAPHQL;
    }
}
