package com.studio.vuepage.dsl;

import com.studio.vuepage.dsl.model.DslNode;
import com.studio.vuepage.dsl.model.PageDsl;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * modify 场景下的 id 对齐：尽量保留 before 中稳定 id，新节点分配 {@code {type}_{8chars}}。
 * <p>
 * 算法见设计 §7.5：
 * <ol>
 *   <li>按顺序 + type 对齐节点列表</li>
 *   <li>类型匹配时强制使用旧节点 id</li>
 *   <li>数组 props（columns/fields/items/actions/rowActions）先按 id，再按 prop/name 对齐并恢复旧 id</li>
 *   <li>新节点生成 {@code {type}_{8 hex}}</li>
 *   <li>递归 Container.children 与 two-column 的 left/right</li>
 *   <li>最终全局唯一性修复</li>
 * </ol>
 */
public final class DslIdReconciler {

    private static final String[] ARRAY_PROP_KEYS = {
            "columns", "fields", "items", "actions", "rowActions", "tags", "data"
    };

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /**
     * 以 {@code before} 为权威 id 来源，对齐 {@code incoming} 并返回新的 PageDsl（不修改入参）。
     */
    public PageDsl reconcile(PageDsl before, PageDsl incoming) {
        if (incoming == null) {
            throw new IllegalArgumentException("incoming must not be null");
        }
        PageDsl result = deepCopy(incoming);
        List<DslNode> beforeChildren = before != null && before.getChildren() != null
                ? before.getChildren()
                : List.of();
        List<DslNode> resultChildren = result.getChildren() != null
                ? result.getChildren()
                : new ArrayList<>();
        if (result.getChildren() == null) {
            result.setChildren(resultChildren);
        }

        reconcileNodeList(beforeChildren, resultChildren);
        ensureGlobalUniqueIds(result);
        return result;
    }

    private void reconcileNodeList(List<DslNode> before, List<DslNode> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return;
        }
        List<DslNode> beforeList = before != null ? before : List.of();
        boolean[] used = new boolean[beforeList.size()];

        for (int i = 0; i < incoming.size(); i++) {
            DslNode inc = incoming.get(i);
            if (inc == null) {
                continue;
            }
            DslNode match = null;
            // 优先：同下标 + type 一致
            if (i < beforeList.size()
                    && !used[i]
                    && typeEquals(beforeList.get(i), inc)) {
                match = beforeList.get(i);
                used[i] = true;
            } else {
                // 回退：在未使用的 before 中找第一个同 type（保持顺序扫描）
                for (int j = 0; j < beforeList.size(); j++) {
                    if (!used[j] && typeEquals(beforeList.get(j), inc)) {
                        match = beforeList.get(j);
                        used[j] = true;
                        break;
                    }
                }
            }

            if (match != null) {
                if (match.getId() != null && !match.getId().isBlank()) {
                    inc.setId(match.getId());
                } else if (isBlank(inc.getId())) {
                    inc.setId(newId(inc.getType()));
                }
                reconcileArrayProps(match.getProps(), inc.getProps());
                reconcileNestedNodes(match, inc);
            } else {
                if (isBlank(inc.getId())) {
                    inc.setId(newId(inc.getType()));
                }
                ensureArrayItemIds(inc.getProps());
                reconcileNestedNodes(null, inc);
            }
        }
    }

    private void reconcileNestedNodes(DslNode before, DslNode incoming) {
        if (incoming == null) {
            return;
        }
        boolean isContainer = "Container".equals(incoming.getType());

        // Container.children
        if (isContainer) {
            List<DslNode> beforeChildren = before != null ? before.getChildren() : null;
            List<DslNode> incChildren = incoming.getChildren();
            if (incChildren != null) {
                reconcileNodeList(beforeChildren, incChildren);
            }
        }

        // two-column: props.left / props.right（Jackson 反序列化为 List<Map>）
        Map<String, Object> beforeProps = before != null ? before.getProps() : null;
        Map<String, Object> incProps = incoming.getProps();
        if (incProps != null && isContainer) {
            reconcileSideNodes(beforeProps, incProps, "left");
            reconcileSideNodes(beforeProps, incProps, "right");
        }
    }

    @SuppressWarnings("unchecked")
    private void reconcileSideNodes(
            Map<String, Object> beforeProps,
            Map<String, Object> incProps,
            String sideKey
    ) {
        Object rawInc = incProps.get(sideKey);
        if (!(rawInc instanceof List<?> incList) || incList.isEmpty()) {
            return;
        }

        Object rawBefore = beforeProps != null ? beforeProps.get(sideKey) : null;
        List<DslNode> beforeNodes = toDslNodeList(rawBefore);
        List<DslNode> incNodes = toDslNodeList(rawInc);

        reconcileNodeList(beforeNodes, incNodes);

        // 将对齐后的 id / 嵌套写回 props 中的原始 list（Map 或 DslNode）
        for (int i = 0; i < incList.size() && i < incNodes.size(); i++) {
            Object item = incList.get(i);
            DslNode reconciled = incNodes.get(i);
            if (item instanceof Map<?, ?> map) {
                writeNodeIntoMap((Map<String, Object>) map, reconciled);
            } else if (item instanceof DslNode node) {
                node.setId(reconciled.getId());
                node.setType(reconciled.getType());
                node.setProps(reconciled.getProps());
                node.setChildren(reconciled.getChildren());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void writeNodeIntoMap(Map<String, Object> map, DslNode node) {
        map.put("id", node.getId());
        if (node.getType() != null) {
            map.put("type", node.getType());
        }
        if (node.getProps() != null) {
            map.put("props", node.getProps());
        }
        if (node.getChildren() != null) {
            map.put("children", node.getChildren());
        }
    }

    @SuppressWarnings("unchecked")
    private static List<DslNode> toDslNodeList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<DslNode> nodes = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof DslNode n) {
                nodes.add(n);
            } else if (item instanceof Map<?, ?> m) {
                nodes.add(mapToNode((Map<String, Object>) m));
            }
        }
        return nodes;
    }

    @SuppressWarnings("unchecked")
    private static DslNode mapToNode(Map<String, Object> map) {
        DslNode node = new DslNode();
        Object id = map.get("id");
        Object type = map.get("type");
        Object props = map.get("props");
        Object children = map.get("children");
        node.setId(id != null ? String.valueOf(id) : null);
        node.setType(type != null ? String.valueOf(type) : null);
        if (props instanceof Map<?, ?> pm) {
            // 浅层拷贝，内部 list/map 仍与原 props 共享，便于回写 id
            node.setProps(new LinkedHashMap<>((Map<String, Object>) pm));
            // 但 setProps 会再拷贝一层——需要让 array 仍是原引用
            // 重新挂回原 map 中的可变结构
            Map<String, Object> shared = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : ((Map<String, Object>) pm).entrySet()) {
                shared.put(e.getKey(), e.getValue());
            }
            // 直接替换内部 props 引用：绕过 setProps 的拷贝语义
            node.setProps(shared);
            // setProps 仍会 new LinkedHashMap — 对 list 值是浅拷贝引用，OK
        }
        if (children instanceof List<?> cl) {
            List<DslNode> childNodes = new ArrayList<>();
            for (Object c : cl) {
                if (c instanceof DslNode n) {
                    childNodes.add(n);
                } else if (c instanceof Map<?, ?> cm) {
                    childNodes.add(mapToNode((Map<String, Object>) cm));
                }
            }
            node.setChildren(childNodes);
        }
        return node;
    }

    @SuppressWarnings("unchecked")
    private void reconcileArrayProps(Map<String, Object> beforeProps, Map<String, Object> incProps) {
        if (incProps == null) {
            return;
        }
        Map<String, Object> bProps = beforeProps != null ? beforeProps : Map.of();

        for (String key : ARRAY_PROP_KEYS) {
            Object bVal = bProps.get(key);
            Object iVal = incProps.get(key);
            // FormSection.props.columns 可能是数字，跳过非数组
            if (!(iVal instanceof List<?>)) {
                continue;
            }
            List<?> beforeItems = bVal instanceof List<?> bl ? bl : List.of();
            List<?> incItems = (List<?>) iVal;
            reconcileItemList(beforeItems, incItems);
        }
    }

    @SuppressWarnings("unchecked")
    private void ensureArrayItemIds(Map<String, Object> props) {
        if (props == null) {
            return;
        }
        for (String key : ARRAY_PROP_KEYS) {
            Object val = props.get(key);
            if (!(val instanceof List<?> list)) {
                continue;
            }
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    Map<String, Object> map = (Map<String, Object>) m;
                    if (isBlank(str(map.get("id")))) {
                        map.put("id", newId(guessItemType(key)));
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void reconcileItemList(List<?> beforeItems, List<?> incItems) {
        List<Map<String, Object>> beforeMaps = new ArrayList<>();
        for (Object o : beforeItems) {
            if (o instanceof Map<?, ?> m) {
                beforeMaps.add((Map<String, Object>) m);
            }
        }
        boolean[] used = new boolean[beforeMaps.size()];

        for (Object o : incItems) {
            if (!(o instanceof Map<?, ?>)) {
                continue;
            }
            Map<String, Object> inc = (Map<String, Object>) o;
            Map<String, Object> match = null;

            // 1) 按 id 对齐
            String incId = str(inc.get("id"));
            if (!isBlank(incId)) {
                for (int j = 0; j < beforeMaps.size(); j++) {
                    if (used[j]) {
                        continue;
                    }
                    if (incId.equals(str(beforeMaps.get(j).get("id")))) {
                        match = beforeMaps.get(j);
                        used[j] = true;
                        break;
                    }
                }
            }

            // 2) 按 prop / name 对齐
            if (match == null) {
                String key = firstNonBlank(str(inc.get("prop")), str(inc.get("name")));
                if (!isBlank(key)) {
                    for (int j = 0; j < beforeMaps.size(); j++) {
                        if (used[j]) {
                            continue;
                        }
                        Map<String, Object> b = beforeMaps.get(j);
                        if (key.equals(str(b.get("prop"))) || key.equals(str(b.get("name")))) {
                            match = b;
                            used[j] = true;
                            break;
                        }
                    }
                }
            }

            if (match != null) {
                String oldId = str(match.get("id"));
                if (!isBlank(oldId)) {
                    inc.put("id", oldId);
                } else if (isBlank(str(inc.get("id")))) {
                    inc.put("id", newId("item"));
                }
            } else if (isBlank(str(inc.get("id")))) {
                String keyHint = firstNonBlank(str(inc.get("prop")), str(inc.get("name")), "item");
                inc.put("id", newId(sanitizeType(keyHint)));
            }
        }
    }

    private void ensureGlobalUniqueIds(PageDsl dsl) {
        Set<String> seen = new HashSet<>();
        if (dsl.getChildren() == null) {
            return;
        }
        for (DslNode child : dsl.getChildren()) {
            uniquifyNode(child, seen);
        }
    }

    @SuppressWarnings("unchecked")
    private void uniquifyNode(DslNode node, Set<String> seen) {
        if (node == null) {
            return;
        }
        String type = node.getType() != null ? node.getType() : "Node";
        node.setId(uniqueId(node.getId(), type, seen));

        Map<String, Object> props = node.getProps();
        if (props != null) {
            for (String key : ARRAY_PROP_KEYS) {
                Object val = props.get(key);
                if (!(val instanceof List<?> list)) {
                    continue;
                }
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        Map<String, Object> map = (Map<String, Object>) m;
                        String itemType = guessItemType(key);
                        String id = str(map.get("id"));
                        map.put("id", uniqueId(id, itemType, seen));
                    }
                }
            }
            // left / right 中的节点
            uniquifySide(props.get("left"), seen);
            uniquifySide(props.get("right"), seen);
        }

        if (node.getChildren() != null) {
            for (DslNode c : node.getChildren()) {
                uniquifyNode(c, seen);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void uniquifySide(Object raw, Set<String> seen) {
        if (!(raw instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            if (item instanceof DslNode n) {
                uniquifyNode(n, seen);
            } else if (item instanceof Map<?, ?> m) {
                DslNode converted = mapToNode((Map<String, Object>) m);
                uniquifyNode(converted, seen);
                writeNodeIntoMap((Map<String, Object>) m, converted);
            }
        }
    }

    private String uniqueId(String current, String type, Set<String> seen) {
        String id = current;
        if (isBlank(id)) {
            id = newId(type);
        }
        if (seen.add(id)) {
            return id;
        }
        // 冲突：重新生成
        String regenerated;
        do {
            regenerated = newId(type);
        } while (!seen.add(regenerated));
        return regenerated;
    }

    static String newId(String type) {
        return sanitizeType(type) + "_" + randomHex8();
    }

    private static String sanitizeType(String type) {
        if (isBlank(type)) {
            return "Node";
        }
        // 仅保留字母数字，避免非法 id
        String cleaned = type.replaceAll("[^A-Za-z0-9_]", "");
        if (cleaned.isEmpty()) {
            return "Node";
        }
        if (!Character.isLetter(cleaned.charAt(0))) {
            return "N" + cleaned;
        }
        return cleaned;
    }

    private static String randomHex8() {
        char[] buf = new char[8];
        for (int i = 0; i < 8; i++) {
            buf[i] = HEX[RANDOM.nextInt(HEX.length)];
        }
        return new String(buf);
    }

    private static String guessItemType(String arrayKey) {
        return switch (arrayKey) {
            case "columns" -> "col";
            case "fields" -> "field";
            case "items" -> "item";
            case "actions" -> "act";
            case "rowActions" -> "ra";
            default -> "item";
        };
    }

    private static boolean typeEquals(DslNode a, DslNode b) {
        if (a == null || b == null) {
            return false;
        }
        return Objects.equals(a.getType(), b.getType()) && a.getType() != null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (!isBlank(v)) {
                return v;
            }
        }
        return null;
    }

    private static PageDsl deepCopy(PageDsl source) {
        try {
            return DslJsonSupport.mapper().readValue(
                    DslJsonSupport.mapper().writeValueAsBytes(source),
                    PageDsl.class);
        } catch (Exception e) {
            throw new IllegalStateException("failed to deep-copy PageDsl", e);
        }
    }
}
