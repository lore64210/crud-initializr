package com.example.projectBase.builder;

import com.example.projectBase.domain.GenericFile;


public class GenericFileBuilder {

    private GenericFile instance;

    private GenericFileBuilder() {
        instance = new GenericFile();
    }

    public GenericFile build() {
        return this.instance;
    }

    public static GenericFileBuilder generic() {
        GenericFileBuilder builder = new GenericFileBuilder();
        builder.withProjectOwner(null);
	builder.withUrl(null);
	
        return builder;
    }

    public GenericFileBuilder withProjectOwner(String val) {
	instance.setProjectOwner(val);
	return this;
}

public GenericFileBuilder withUrl(String val) {
	instance.setUrl(val);
	return this;
}



}
