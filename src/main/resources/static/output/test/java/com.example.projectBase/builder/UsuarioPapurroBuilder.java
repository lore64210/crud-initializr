package com.example.projectBase.builder;

import com.example.projectBase.domain.UsuarioPapurro;


public class UsuarioPapurroBuilder {

    private UsuarioPapurro instance;

    private UsuarioPapurroBuilder() {
        instance = new UsuarioPapurro();
    }

    public UsuarioPapurro build() {
        return this.instance;
    }

    public static UsuarioPapurroBuilder generic() {
        UsuarioPapurroBuilder builder = new UsuarioPapurroBuilder();
        builder.withNiIdea(null);
	
        return builder;
    }

    public UsuarioPapurroBuilder withNiIdea(Long val) {
	instance.setNiIdea(val);
	return this;
}



}
