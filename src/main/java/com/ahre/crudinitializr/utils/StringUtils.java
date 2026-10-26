package com.ahre.crudinitializr.utils;

public final class StringUtils {

    private StringUtils() {
    }

    public static String capitalizeFirstCharacter(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    public static String lowerFirstCharacter(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toLowerCase() + value.substring(1);
    }

    public static String transformToSnakeCase(String str) {
        String regex = "([a-z])([A-Z]+)";
        String replacement = "$1_$2";
        str = str.trim().replaceAll("\\s", "_").replaceAll(regex, replacement).toLowerCase();
        return str;
    }

    public static String toArtifactId(String value) {
        String artifactId = value == null ? "" : value.trim().toLowerCase()
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+|-+$", "");
        return artifactId.isEmpty() ? "project-base" : artifactId;
    }

    public static String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
