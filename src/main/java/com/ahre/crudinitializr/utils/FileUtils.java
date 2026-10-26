package com.ahre.crudinitializr.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

@Component
public final class FileUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);
    private static final String TEMPLATE_ROOT = "projectBase";

    public String getTemplateContent(String relativePath) {
        Resource resource = templateResource(relativePath);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("No se encontró la plantilla: " + relativePath, e);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la plantilla: " + relativePath, e);
        }
    }

    public void copyTemplate(String relativePath, Path destinationFolder) throws IOException {
        copyTemplate(relativePath, destinationFolder, normalize(relativePath));
    }

    public void copyTemplate(String relativePath, Path destinationFolder, String destinationRelativePath) throws IOException {
        Resource resource = templateResource(relativePath);
        Path destination = destinationFolder.resolve(destinationRelativePath);
        try (InputStream inputStream = resource.getInputStream()) {
            if (destination.getParent() != null) {
                Files.createDirectories(destination.getParent());
            }
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("No se encontró la plantilla: " + relativePath, e);
        }
    }

    public Path createOutputFolder() throws IOException {
        return Files.createTempDirectory("crud-initializr-");
    }

    public void writeFile(String pathToFile, String fileContent) throws IOException {
        Path path = Paths.get(pathToFile);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, fileContent, StandardCharsets.UTF_8);
    }

    public void deleteDirectory(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(current -> {
                try {
                    Files.delete(current);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public void deleteQuietly(Path path) {
        try {
            deleteDirectory(path);
        } catch (IOException e) {
            LOGGER.warn("No se pudo eliminar el directorio temporal {}", path, e);
        }
    }

    private Resource templateResource(String relativePath) {
        return new ClassPathResource(TEMPLATE_ROOT + "/" + normalize(relativePath));
    }

    private String normalize(String relativePath) {
        return relativePath.replace('\\', '/').replaceFirst("^/+", "");
    }
}
