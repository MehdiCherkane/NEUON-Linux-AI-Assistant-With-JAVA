package com.neuon.tools;

import java.nio.file.Path;

public final class WorkspacePaths {
    private static final Path ROOT = Path.of(
        System.getenv().getOrDefault(
            "NEUON_WORKSPACE",
            Path.of(System.getProperty("user.home"), "Desktop", "workspace").toString()
        )
    ).toAbsolutePath().normalize();

    private WorkspacePaths() {
    }

    public static Path root() {
        return ROOT;
    }

    public static Path resolveInsideWorkspace(String requestedPath) {
        Path path = Path.of(requestedPath);
        Path resolved = path.isAbsolute() ? path : ROOT.resolve(path);
        resolved = resolved.toAbsolutePath().normalize();

        if (!resolved.startsWith(ROOT)) {
            throw new IllegalArgumentException("Path must stay inside workspace: " + ROOT);
        }

        return resolved;
    }
}
