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

    public static String toClassName(String value) {
        String artifactId = toArtifactId(value);
        StringBuilder className = new StringBuilder();

        for (String part : artifactId.split("[^a-z0-9]+")) {
            if (!part.isEmpty()) {
                className.append(capitalizeFirstCharacter(part));
            }
        }

        if (className.isEmpty()) {
            return "ProjectBase";
        }

        if (!Character.isJavaIdentifierStart(className.charAt(0))) {
            className.insert(0, "Project");
        }

        return className.toString();
    }

    public static String toDatabaseName(String value) {
        String databaseName = toArtifactId(value)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^_+|_+$", "");

        if (databaseName.isEmpty()) {
            return "project_base";
        }

        if (!databaseName.matches("^[a-z_].*")) {
            return "db_" + databaseName;
        }

        return databaseName;
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
