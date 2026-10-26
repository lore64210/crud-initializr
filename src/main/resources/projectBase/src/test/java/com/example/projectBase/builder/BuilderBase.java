package com.example.projectBase.builder;

IMPORT

public class DOMAINBuilder {

    private DOMAIN instance;

    private DOMAINBuilder() {
        instance = new DOMAIN();
    }

    public DOMAIN build() {
        return this.instance;
    }

    public static DOMAINBuilder generic() {
        DOMAINBuilder builder = new DOMAINBuilder();
        GENERIC_BUILD
        return builder;
    }

    GENERIC_METHODS

}
