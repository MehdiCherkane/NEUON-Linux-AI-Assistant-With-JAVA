package com.neuon.tools;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;

class ToolWareHouseTest {

    @Test
    void allToolsAreExposed() {
        ToolWareHouse wh = new ToolWareHouse();
        ArrayList<String> names = wh.getAllToolsNames();
        assertNotNull(names);
        assertFalse(names.isEmpty());
        assertTrue(names.contains("run_shell"));
        assertTrue(names.contains("make_file"));
        assertTrue(names.contains("invoke_code_agent"));
    }

    @Test
    void allToolsNamesIncludeNewTools() {
        ToolWareHouse wh = new ToolWareHouse();
        ArrayList<String> names = wh.getAllToolsNames();
        assertTrue(names.contains("list_files"), "Should contain list_files");
        assertTrue(names.contains("edit_file"), "Should contain edit_file");
    }

    @Test
    void getAllToolsReturnsAll() {
        ToolWareHouse wh = new ToolWareHouse();
        ToolRegistry all = wh.getAllTools();
        assertNotNull(all);
        assertNotNull(all.toJson());
        assertTrue(all.toJson().size() > 0);
    }

    @Test
    void getNeededToolsByListReturnsSubset() {
        ToolWareHouse wh = new ToolWareHouse();
        ArrayList<String> subset = new ArrayList<>(Arrays.asList("run_shell", "make_file"));
        ToolRegistry registry = wh.getNeededTools(subset);
        assertEquals(2, registry.toJson().size());
    }

    @Test
    void getNeededToolsByListFiltersUnknown() {
        ToolWareHouse wh = new ToolWareHouse();
        ArrayList<String> withUnknown = new ArrayList<>(Arrays.asList("run_shell", "nonexistent_tool"));
        ToolRegistry registry = wh.getNeededTools(withUnknown);
        assertEquals(1, registry.toJson().size());
    }

    @Test
    void codeAgentToolsExist() {
        ToolWareHouse wh = new ToolWareHouse();
        ArrayList<String> codeTools = new ArrayList<>(
            java.util.List.of("make_project_directory", "make_file", "run_shell", "list_files", "read_file", "edit_file")
        );
        ToolRegistry registry = wh.getNeededTools(codeTools);
        assertEquals(6, registry.toJson().size());
    }
}
