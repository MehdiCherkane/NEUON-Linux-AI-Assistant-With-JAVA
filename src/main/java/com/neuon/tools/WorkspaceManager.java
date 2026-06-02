package com.neuon.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class WorkspaceManager {

    private WorkspaceManager() {
    }

    public static Path root() {
        return WorkspacePaths.root();
    }

    public static Path resolve(String requestedPath) {
        return WorkspacePaths.resolveInsideWorkspace(requestedPath);
    }

    public static Path createProject(String projectName) {
        String sanitized = projectName.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (sanitized.isBlank()) {
            throw new IllegalArgumentException("Invalid project name: " + projectName);
        }
        Path projectDir = WorkspacePaths.resolveInsideWorkspace(sanitized);
        try {
            Files.createDirectories(projectDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create project directory: " + e.getMessage(), e);
        }
        return projectDir;
    }

    public static String listFiles(String dirPath) {
        Path resolved = WorkspacePaths.resolveInsideWorkspace(dirPath);
        if (!Files.isDirectory(resolved)) {
            return "Error: path is not a directory: " + resolved;
        }
        try (Stream<Path> stream = Files.list(resolved)) {
            List<Path> files = stream.sorted().collect(Collectors.toList());
            if (files.isEmpty()) {
                return "Directory is empty: " + resolved;
            }
            StringBuilder sb = new StringBuilder();
            for (Path f : files) {
                String type = Files.isDirectory(f) ? "[DIR] " : "[FILE] ";
                sb.append(type).append(f.getFileName()).append("\n");
            }
            return sb.toString().stripTrailing();
        } catch (IOException e) {
            return "Error listing files: " + e.getMessage();
        }
    }

    public static String readFile(String filePath) {
        Path resolved = WorkspacePaths.resolveInsideWorkspace(filePath);
        if (!Files.exists(resolved)) {
            return "Error: file not found: " + resolved;
        }
        if (!Files.isRegularFile(resolved)) {
            return "Error: not a regular file: " + resolved;
        }
        try {
            return Files.readString(resolved);
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    public static String writeFile(String filePath, String content) {
        Path resolved = WorkspacePaths.resolveInsideWorkspace(filePath);
        try {
            Path parent = resolved.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(resolved, content);
            return "File written: " + resolved;
        } catch (IOException e) {
            return "Error writing file: " + e.getMessage();
        }
    }
}
