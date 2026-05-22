package com.codesync.file.util;

public class PathUtil {

    private PathUtil() {
    }

    public static String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "";
        }
        path = path.trim().replace("\\", "/");
        while (path.contains("//")) {
            path = path.replace("//", "/");
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    public static String buildFolderPath(String parentPath, String folderName) {
        parentPath = normalizePath(parentPath);
        if (!parentPath.isEmpty() && !parentPath.endsWith("/")) {
            parentPath += "/";
        }
        return parentPath + folderName + "/";
    }

    public static String buildFilePath(String parentPath, String fileName) {
        parentPath = normalizePath(parentPath);
        if (!parentPath.isEmpty() && !parentPath.endsWith("/")) {
            parentPath += "/";
        }
        return parentPath + fileName;
    }

    public static String getParentPath(String fullPath) {
        fullPath = normalizePath(fullPath);
        int index = fullPath.lastIndexOf("/");
        if (index == -1) {
            return "";
        }
        return fullPath.substring(0, index);
    }
}