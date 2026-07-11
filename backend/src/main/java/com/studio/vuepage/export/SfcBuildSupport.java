package com.studio.vuepage.export;

import com.studio.vuepage.dsl.model.DslNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ElementPlus / Plain 导出器共享的辅助方法。
 */
final class SfcBuildSupport {

    private SfcBuildSupport() {
    }

    /** 将节点 id 转为合法 JS 标识符片段 */
    static String safeJsId(String id) {
        if (id == null || id.isBlank()) {
            return "node";
        }
        StringBuilder sb = new StringBuilder(id.length());
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        String s = sb.toString();
        if (s.isEmpty() || Character.isDigit(s.charAt(0))) {
            return "n_" + s;
        }
        return s;
    }

    static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    static String propString(Map<String, Object> props, String key, String defaultValue) {
        if (props == null || !props.containsKey(key) || props.get(key) == null) {
            return defaultValue;
        }
        return String.valueOf(props.get(key));
    }

    static int propInt(Map<String, Object> props, String key, int defaultValue) {
        if (props == null) {
            return defaultValue;
        }
        Object v = props.get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v != null) {
            try {
                return Integer.parseInt(String.valueOf(v));
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> listOfMaps(Object value) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!(value instanceof List<?> list)) {
            return out;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                out.add(new LinkedHashMap<>((Map<String, Object>) m));
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    static List<DslNode> asNodes(Object value) {
        List<DslNode> out = new ArrayList<>();
        if (!(value instanceof List<?> list)) {
            return out;
        }
        for (Object item : list) {
            if (item instanceof DslNode n) {
                out.add(n);
            } else if (item instanceof Map<?, ?> map) {
                out.add(mapToNode((Map<String, Object>) map));
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    static DslNode mapToNode(Map<String, Object> map) {
        DslNode node = new DslNode();
        Object id = map.get("id");
        Object type = map.get("type");
        Object props = map.get("props");
        Object children = map.get("children");
        node.setId(id != null ? String.valueOf(id) : null);
        node.setType(type != null ? String.valueOf(type) : null);
        if (props instanceof Map<?, ?> pm) {
            node.setProps((Map<String, Object>) pm);
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

    /** 生成约 3 行 mock 行数据（JS 对象字面量，不含外层 []） */
    static String buildMockRowsJs(List<Map<String, Object>> columns, AtomicInteger seed) {
        String[][] sampleValues = {
                {"示例甲", "示例乙", "示例丙"},
                {"a@example.com", "b@example.com", "c@example.com"},
                {"启用", "禁用", "启用"},
                {"10", "20", "30"},
                {"2026-01-01", "2026-01-02", "2026-01-03"}
        };
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < 3; row++) {
            if (row > 0) {
                sb.append(",\n  ");
            }
            sb.append("{ ");
            for (int c = 0; c < columns.size(); c++) {
                if (c > 0) {
                    sb.append(", ");
                }
                String prop = str(columns.get(c).get("prop"));
                if (prop.isBlank()) {
                    prop = "col" + c;
                }
                String val = sampleValues[(c + seed.get()) % sampleValues.length][row];
                sb.append(prop).append(": ").append(jsString(val));
            }
            sb.append(" }");
        }
        seed.incrementAndGet();
        return sb.toString();
    }

    /** JS 字符串字面量转义 */
    static String jsString(String s) {
        if (s == null) {
            return "''";
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        sb.append('\'');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\'' -> sb.append("\\'");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        sb.append('\'');
        return sb.toString();
    }

    static void appendIndent(StringBuilder sb, int indent) {
        sb.append("  ".repeat(Math.max(0, indent)));
    }
}
