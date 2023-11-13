package com.ahre.crudinitializr.utils;

public final class StringUtils {

    public static String capitalizeFirstCharacter(String value) {
        return value.substring(0,1).toUpperCase() + value.substring(1);
    }

    public static String transformToSnakeCase(String str) {
        String regex = "([a-z])([A-Z]+)";
        String replacement = "$1_$2";
        str = str.trim().replaceAll("\\s", "_").replaceAll(regex, replacement).toLowerCase();
        return str;
    }

    public static String replaceLast(String string, String substring, String replacement) {
        int index = string.lastIndexOf(substring);
        if (index == -1)
            return string;
        return string.substring(0, index) + replacement
                + string.substring(index+substring.length());
    }

}
