package com.example.projectBase.builder;

import com.example.projectBase.domain.EntrevistaPrincipal;
import com.example.projectBase.domain.UsuarioPapurro;


public class EntrevistaPrincipalBuilder {

    private EntrevistaPrincipal instance;

    private EntrevistaPrincipalBuilder() {
        instance = new EntrevistaPrincipal();
    }

    public EntrevistaPrincipal build() {
        return this.instance;
    }

    public static EntrevistaPrincipalBuilder generic() {
        EntrevistaPrincipalBuilder builder = new EntrevistaPrincipalBuilder();
        builder.withTituloChevere(null);
	builder.withUsuarioPapurro(null);
	
        return builder;
    }

    public EntrevistaPrincipalBuilder withTituloChevere(String val) {
	instance.setTituloChevere(val);
	return this;
}

public EntrevistaPrincipalBuilder withUsuarioPapurro(UsuarioPapurro val) {
	instance.setUsuarioPapurro(val);
	return this;
}



}
