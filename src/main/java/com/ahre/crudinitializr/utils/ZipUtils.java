package com.ahre.crudinitializr.utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ZipUtils {

    private ZipUtils() {
    }

    public static void zipFolder(Path folder, OutputStream outputStream) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            addEntries(folder, folder, zipOutputStream);
        }
    }

    private static void addEntries(Path root, Path folder, ZipOutputStream zipOutputStream) throws IOException {
        try (Stream<Path> entries = Files.list(folder)) {
            for (Path entry : entries.sorted().toList()) {
                if (Files.isDirectory(entry)) {
                    addEntries(root, entry, zipOutputStream);
                    continue;
                }
                String entryName = root.relativize(entry).toString().replace(File.separatorChar, '/');
                zipOutputStream.putNextEntry(new ZipEntry(entryName));
                Files.copy(entry, zipOutputStream);
                zipOutputStream.closeEntry();
            }
        }
    }
}
