package com.ahre.crudinitializr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Resolves the response language from the standard {@code Accept-Language}
 * header. Spanish is the default so existing clients keep working unchanged.
 */
@Configuration
public class LocaleConfig {

    private static final Locale SPANISH = Locale.forLanguageTag("es");

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(SPANISH, Locale.ENGLISH));
        resolver.setDefaultLocale(SPANISH);
        return resolver;
    }
}
