package com.studio.vuepage.dsl;

import com.studio.vuepage.dsl.model.DslNode;
import com.studio.vuepage.dsl.model.PageDsl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DslValidatorTest {

    private DslValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DslValidator();
    }

    @Test
    void acceptsListUserFixture() {
        PageDsl dsl = DslJsonSupport.readClasspath("fixtures/dsl/list-user.json");
        List<String> errors = validator.validate(dsl);
        assertTrue(errors.isEmpty(), () -> "expected valid, got: " + errors);
    }

    @Test
    void acceptsFormUserFixture() {
        PageDsl dsl = DslJsonSupport.readClasspath("fixtures/dsl/form-user.json");
        List<String> errors = validator.validate(dsl);
        assertTrue(errors.isEmpty(), () -> "expected valid, got: " + errors);
    }

    @Test
    void acceptsDashboardMinFixture() {
        PageDsl dsl = DslJsonSupport.readClasspath("fixtures/dsl/dashboard-min.json");
        List<String> errors = validator.validate(dsl);
        assertTrue(errors.isEmpty(), () -> "expected valid, got: " + errors);
    }

    @Test
    void rejectsUnknownType() {
        PageDsl dsl = DslJsonSupport.readClasspath("fixtures/dsl/list-user.json");
        dsl.getChildren().get(0).setType("UnknownWidget");
        List<String> errors = validator.validate(dsl);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.toLowerCase().contains("type") || e.contains("UnknownWidget")),
                () -> "expected type error, got: " + errors);
    }

    @Test
    void rejectsDuplicateIds() {
        PageDsl dsl = DslJsonSupport.readClasspath("fixtures/dsl/list-user.json");
        String dupId = dsl.getChildren().get(0).getId();
        dsl.getChildren().get(1).setId(dupId);
        List<String> errors = validator.validate(dsl);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.toLowerCase().contains("id") || e.contains("unique") || e.contains("重复")),
                () -> "expected duplicate id error, got: " + errors);
    }

    @Test
    void rejectsChildrenOnNonContainer() {
        PageDsl dsl = DslJsonSupport.readClasspath("fixtures/dsl/list-user.json");
        DslNode leaf = dsl.getChildren().get(0); // PageHeader
        DslNode child = new DslNode();
        child.setId("extra_child_01");
        child.setType("TextBlock");
        child.setProps(Map.of("content", "x"));
        leaf.setChildren(List.of(child));
        List<String> errors = validator.validate(dsl);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e ->
                        e.toLowerCase().contains("children") || e.contains("Container")),
                () -> "expected children-on-non-container error, got: " + errors);
    }

    @Test
    void rejectsBadFieldName() {
        PageDsl dsl = DslJsonSupport.readClasspath("fixtures/dsl/list-user.json");
        // DataTable is children[2]
        DslNode table = dsl.getChildren().get(2);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = (List<Map<String, Object>>) table.getProps().get("columns");
        Map<String, Object> firstCol = new LinkedHashMap<>(columns.get(0));
        firstCol.put("prop", "bad-name");
        List<Map<String, Object>> newCols = new ArrayList<>();
        newCols.add(firstCol);
        for (int i = 1; i < columns.size(); i++) {
            newCols.add(columns.get(i));
        }
        Map<String, Object> props = new HashMap<>(table.getProps());
        props.put("columns", newCols);
        table.setProps(props);

        List<String> errors = validator.validate(dsl);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e ->
                        e.contains("bad-name") || e.toLowerCase().contains("prop") || e.contains("name")),
                () -> "expected bad field name error, got: " + errors);
    }

    @Test
    void rejectsWrongVersion() {
        PageDsl dsl = DslJsonSupport.readClasspath("fixtures/dsl/list-user.json");
        dsl.setVersion("2");
        List<String> errors = validator.validate(dsl);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.toLowerCase().contains("version")),
                () -> "expected version error, got: " + errors);
    }
}
