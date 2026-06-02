package com.neuon.tools;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

class WorkspacePathsTest {

    @Test
    void rootUsesEnvVarWhenSet() {
        Path root = WorkspacePaths.root();
        assertNotNull(root);
        assertTrue(root.isAbsolute());
    }

    @Test
    void resolvesRelativePathInsideWorkspace() {
        Path resolved = WorkspacePaths.resolveInsideWorkspace("myproject/test.txt");
        assertTrue(resolved.toString().endsWith("myproject/test.txt"));
        assertTrue(resolved.startsWith(WorkspacePaths.root()));
    }

    @Test
    void resolvesAbsolutePathInsideWorkspace() {
        Path root = WorkspacePaths.root();
        Path inside = root.resolve("somefile.java");
        Path resolved = WorkspacePaths.resolveInsideWorkspace(inside.toString());
        assertEquals(inside.normalize(), resolved);
    }

    @Test
    void rejectsPathOutsideWorkspace() {
        String outside = "/etc/passwd";
        assertThrows(IllegalArgumentException.class, () -> {
            WorkspacePaths.resolveInsideWorkspace(outside);
        });
    }

    @Test
    void rejectsPathWithDoubleDotEscape() {
        assertThrows(IllegalArgumentException.class, () -> {
            WorkspacePaths.resolveInsideWorkspace("../outside");
        });
    }

    @Test
    void rejectsPathWithDeepDoubleDotEscape() {
        assertThrows(IllegalArgumentException.class, () -> {
            WorkspacePaths.resolveInsideWorkspace("project/../../outside");
        });
    }

    @Test
    void resolvesDotInPath() {
        Path resolved = WorkspacePaths.resolveInsideWorkspace("./myproject");
        assertEquals(WorkspacePaths.root().resolve("myproject").normalize(), resolved);
    }

    @Test
    void rootIsNormalized() {
        Path root = WorkspacePaths.root();
        assertEquals(root, root.normalize());
    }
}
