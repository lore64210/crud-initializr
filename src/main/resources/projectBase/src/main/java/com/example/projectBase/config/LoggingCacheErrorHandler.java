package com.example.projectBase.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

public class LoggingCacheErrorHandler implements CacheErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        LOGGER.warn("No se pudo leer del cache '{}' con la clave '{}'. Se continúa sin cache.",
                cache.getName(), key, exception);
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        LOGGER.warn("No se pudo escribir en el cache '{}' con la clave '{}'. Se continúa sin cache.",
                cache.getName(), key, exception);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        LOGGER.warn("No se pudo invalidar la clave '{}' del cache '{}'.",
                key, cache.getName(), exception);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        LOGGER.warn("No se pudo limpiar el cache '{}'.", cache.getName(), exception);
    }
}
