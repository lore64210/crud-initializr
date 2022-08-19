package com.ahre.crudinitializr.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

@Component
public final class FileUtils {

    public String getContentFromFile(String path) {
        try {
            File resource = new ClassPathResource(path, this.getClass().getClassLoader()).getFile();
            return new String(Files.readAllBytes(resource.toPath()));
        } catch (IOException e) {
            throw new IllegalArgumentException("Error obteniendo el archivo ", e);
        }
    }

    public void copyFolder(Path src, Path dest) throws IOException {
        try (Stream<Path> stream = Files.walk(src)) {
            stream.forEach(source -> copy(source, dest.resolve(src.relativize(source))));
        }
    }

    public void copy(Path source, Path dest) {
        try {
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void deleteDirectory(Path tmpDir) throws IOException {
        try (Stream<Path> walk = Files.walk(tmpDir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    // something could not be deleted..
                    e.printStackTrace();
                }
            });
        }
    }

    public Path createOutputFolder() throws IOException {
        Path projectBase = new ClassPathResource("/static/projectBase", this.getClass().getClassLoader()).getFile().toPath();
        Path projectPath = new File("src/main/resources/static/output").toPath();
        if (Files.exists(projectPath)) {
            deleteDirectory(projectPath);
        }
        Path project = Files.createDirectory(projectPath);
        copyFolder(projectBase, project);
        return project;
    }

    public void writeFile(String pathToFile, String fileContent) throws IOException {
        Path path = Paths.get(pathToFile);
        if (!Files.exists(path)) {
            Path file = Files.createFile(path);
            Files.writeString(file, fileContent, StandardCharsets.UTF_8);
        }
        Files.writeString(path, fileContent, StandardCharsets.UTF_8);
    }
}
