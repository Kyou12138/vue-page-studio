package com.studio.vuepage.dsl;

import com.studio.vuepage.dsl.model.DslNode;
import com.studio.vuepage.dsl.model.PageDsl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DslIdReconcilerTest {

    private static final Pattern NEW_NODE_ID = Pattern.compile("^[A-Za-z][A-Za-z0-9]*_[A-Za-z0-9]{8}$");

    private DslIdReconciler reconciler;

    @BeforeEach
    void setUp() {
        reconciler = new DslIdReconciler();
    }

    @Test
    void deepCopyHelperUsesObjectMapper() {
        PageDsl original = DslJsonSupport.readClasspath("fixtures/dsl/list-user.json");
        PageDsl copy = deepCopy(original);

        assertNotNull(copy);
        assertEquals(original.getTitle(), copy.getTitle());
        assertEquals(original.getChildren().size(), copy.getChildren().size());
        assertEquals(original.getChildren().get(0).getId(), copy.getChildren().get(0).getId());

        // 深拷贝：修改 copy 不影响 original
        copy.getChildren().get(0).setId("mutated_id_xxxxx");
        assertNotEquals("mutated_id_xxxxx", original.getChildren().get(0).getId());
    }

    @Test
    void preservesColumnIdsWhenPropMatches() {
        PageDsl before = DslJsonSupport.readClasspath("fixtures/dsl/list-user.json");
        PageDsl incoming = deepCopy(before);

        DslNode table = findByType(incoming, "DataTable");
        assertNotNull(table);
        String oldTableId = findByType(before, "DataTable").getId();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> beforeColumns =
                (List<Map<String, Object>>) findByType(before, "DataTable").getProps().get("columns");
        Map<String, String> expectedIdsByProp = new LinkedHashMap<>();
        for (Map<String, Object> col : beforeColumns) {
            expectedIdsByProp.put(String.valueOf(col.get("prop")), String.valueOf(col.get("id")));
        }

        // 打乱 table 与 column 的 id，保留 prop
        table.setId("DataTable_scrambled1");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = (List<Map<String, Object>>) table.getProps().get("columns");
        List<Map<String, Object>> scrambled = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            Map<String, Object> col = new LinkedHashMap<>(columns.get(i));
            col.put("id", "col_scrambled_" + i);
            scrambled.add(col);
        }
        // 新增 mobile 列
        Map<String, Object> mobile = new LinkedHashMap<>();
        mobile.put("id", "col_brand_new99");
        mobile.put("prop", "mobile");
        mobile.put("label", "手机");
        scrambled.add(mobile);

        Map<String, Object> props = new LinkedHashMap<>(table.getProps());
        props.put("columns", scrambled);
        table.setProps(props);

        PageDsl out = reconciler.reconcile(before, incoming);

        DslNode outTable = findByType(out, "DataTable");
        assertNotNull(outTable);
        assertEquals(oldTableId, outTable.getId(), "DataTable id 应按 type 对齐恢复");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> outColumns =
                (List<Map<String, Object>>) outTable.getProps().get("columns");
        assertEquals(4, outColumns.size());

        Map<String, String> outIdsByProp = new LinkedHashMap<>();
        for (Map<String, Object> col : outColumns) {
            outIdsByProp.put(String.valueOf(col.get("prop")), String.valueOf(col.get("id")));
        }

        assertEquals(expectedIdsByProp.get("name"), outIdsByProp.get("name"));
        assertEquals(expectedIdsByProp.get("email"), outIdsByProp.get("email"));
        assertEquals(expectedIdsByProp.get("status"), outIdsByProp.get("status"));
        assertNotNull(outIdsByProp.get("mobile"));
        assertTrue(!outIdsByProp.get("mobile").isBlank());
    }

    @Test
    void assignsNewIdsForBrandNewNodes() {
        PageDsl before = DslJsonSupport.readClasspath("fixtures/dsl/list-user.json");
        PageDsl incoming = deepCopy(before);

        DslNode brandNew = new DslNode();
        brandNew.setType("TextBlock");
        // 故意不设 id / 或设空 id，模拟 LLM 新增节点
        brandNew.setId(null);
        brandNew.setProps(Map.of("content", "提示文案"));
        incoming.getChildren().add(brandNew);

        PageDsl out = reconciler.reconcile(before, incoming);

        DslNode added = out.getChildren().stream()
                .filter(n -> "TextBlock".equals(n.getType()))
                .findFirst()
                .orElse(null);
        assertNotNull(added);
        assertNotNull(added.getId());
        assertTrue(NEW_NODE_ID.matcher(added.getId()).matches(),
                () -> "expected {type}_{8chars}, got: " + added.getId());
        assertTrue(added.getId().startsWith("TextBlock_"));

        // 原有节点 id 保持稳定
        assertEquals(
                findByType(before, "PageHeader").getId(),
                findByType(out, "PageHeader").getId());
    }

    @Test
    void preservesFieldIdsOnFormSectionByName() {
        PageDsl before = DslJsonSupport.readClasspath("fixtures/dsl/form-user.json");
        PageDsl incoming = deepCopy(before);

        DslNode form = findByType(incoming, "FormSection");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) form.getProps().get("fields");
        List<Map<String, Object>> scrambled = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            Map<String, Object> f = new LinkedHashMap<>(fields.get(i));
            f.put("id", "field_x_" + i);
            scrambled.add(f);
        }
        Map<String, Object> props = new LinkedHashMap<>(form.getProps());
        props.put("fields", scrambled);
        form.setProps(props);

        PageDsl out = reconciler.reconcile(before, incoming);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> beforeFields =
                (List<Map<String, Object>>) findByType(before, "FormSection").getProps().get("fields");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> outFields =
                (List<Map<String, Object>>) findByType(out, "FormSection").getProps().get("fields");

        Map<String, String> expected = new LinkedHashMap<>();
        for (Map<String, Object> f : beforeFields) {
            expected.put(String.valueOf(f.get("name")), String.valueOf(f.get("id")));
        }
        for (Map<String, Object> f : outFields) {
            String name = String.valueOf(f.get("name"));
            assertEquals(expected.get(name), f.get("id"), "field id for name=" + name);
        }
    }

    @Test
    void finalPassResolvesDuplicateIds() {
        PageDsl before = DslJsonSupport.readClasspath("fixtures/dsl/list-user.json");
        PageDsl incoming = deepCopy(before);

        // 两个新节点使用相同 id，且无法与 before 对齐
        DslNode a = new DslNode();
        a.setId("dup_same_id01");
        a.setType("TextBlock");
        a.setProps(Map.of("content", "A"));
        DslNode b = new DslNode();
        b.setId("dup_same_id01");
        b.setType("TextBlock");
        b.setProps(Map.of("content", "B"));
        // 替换 children，避免与 before 按顺序 type 对齐
        incoming.setChildren(List.of(a, b));

        PageDsl out = reconciler.reconcile(before, incoming);

        assertEquals(2, out.getChildren().size());
        String id0 = out.getChildren().get(0).getId();
        String id1 = out.getChildren().get(1).getId();
        assertNotNull(id0);
        assertNotNull(id1);
        assertNotEquals(id0, id1, "全局 id 必须唯一");
    }

    /** 通过序列化/反序列化做深拷贝（使用 DslJsonSupport 的 ObjectMapper）。 */
    static PageDsl deepCopy(PageDsl source) {
        try {
            return DslJsonSupport.mapper().readValue(
                    DslJsonSupport.mapper().writeValueAsBytes(source),
                    PageDsl.class);
        } catch (Exception e) {
            throw new IllegalStateException("deepCopy failed", e);
        }
    }

    private static DslNode findByType(PageDsl dsl, String type) {
        if (dsl.getChildren() == null) {
            return null;
        }
        for (DslNode n : dsl.getChildren()) {
            DslNode found = findByType(n, type);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static DslNode findByType(DslNode node, String type) {
        if (node == null) {
            return null;
        }
        if (type.equals(node.getType())) {
            return node;
        }
        if (node.getChildren() != null) {
            for (DslNode c : node.getChildren()) {
                DslNode found = findByType(c, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
