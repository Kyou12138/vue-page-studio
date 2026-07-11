package com.studio.vuepage.dsl;

import com.studio.vuepage.dsl.model.DslNode;
import com.studio.vuepage.dsl.model.PageDsl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PageDSL 运行时校验器。返回错误列表，空列表表示合法。
 */
public class DslValidator {

    public static final int MAX_NODES = 50;
    public static final int MAX_DEPTH = 3;

    private static final Set<String> PAGE_TYPES = Set.of("list", "form", "detail", "dashboard");
    private static final Set<String> STYLES = Set.of("element-plus", "plain");
    private static final Set<String> NODE_TYPES = Set.copyOf(DefaultProps.supportedTypes());
    private static final Set<String> CONTAINER_LAYOUTS = Set.of("stack", "two-column");
    private static final Pattern IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    public List<String> validate(PageDsl dsl) {
        List<String> errors = new ArrayList<>();
        if (dsl == null) {
            errors.add("dsl is null");
            return errors;
        }

        if (!"1".equals(dsl.getVersion())) {
            errors.add("version must be \"1\", got: " + dsl.getVersion());
        }
        if (dsl.getPageType() == null || !PAGE_TYPES.contains(dsl.getPageType())) {
            errors.add("pageType must be one of list|form|detail|dashboard, got: " + dsl.getPageType());
        }
        if (dsl.getStyle() == null || !STYLES.contains(dsl.getStyle())) {
            errors.add("style must be one of element-plus|plain, got: " + dsl.getStyle());
        }
        if (dsl.getTitle() == null || dsl.getTitle().isBlank()) {
            errors.add("title must be non-blank");
        }
        if (dsl.getChildren() == null) {
            errors.add("children must be non-null");
            return errors;
        }

        Set<String> seenIds = new HashSet<>();
        int[] nodeCount = {0};
        for (DslNode child : dsl.getChildren()) {
            walkNode(child, 1, seenIds, nodeCount, errors, "children");
        }
        if (nodeCount[0] > MAX_NODES) {
            errors.add("total node count exceeds limit " + MAX_NODES + ": " + nodeCount[0]);
        }
        return errors;
    }

    private void walkNode(
            DslNode node,
            int depth,
            Set<String> seenIds,
            int[] nodeCount,
            List<String> errors,
            String path
    ) {
        if (node == null) {
            errors.add(path + ": node is null");
            return;
        }
        nodeCount[0]++;
        String nodePath = path + "[" + safeId(node.getId()) + "]";

        if (depth > MAX_DEPTH) {
            errors.add(nodePath + ": depth " + depth + " exceeds limit " + MAX_DEPTH);
        }

        if (node.getId() == null || node.getId().isBlank()) {
            errors.add(nodePath + ": id must be non-empty");
        } else if (!seenIds.add(node.getId())) {
            errors.add(nodePath + ": duplicate id: " + node.getId());
        }

        String type = node.getType();
        if (type == null || type.isBlank()) {
            errors.add(nodePath + ": type is required");
        } else if (!NODE_TYPES.contains(type)) {
            errors.add(nodePath + ": unknown type: " + type);
        }

        Map<String, Object> props = node.getProps();
        if (props == null) {
            errors.add(nodePath + ": props must be non-null");
            props = Map.of();
        }

        boolean isContainer = "Container".equals(type);
        List<DslNode> children = node.getChildren();
        if (!isContainer) {
            if (children != null && !children.isEmpty()) {
                errors.add(nodePath + ": only Container may have children");
            }
        }

        if (type != null && NODE_TYPES.contains(type)) {
            validateTypeSpecific(type, props, nodePath, errors);
        }

        // 递归：Container.children
        if (isContainer && children != null) {
            for (int i = 0; i < children.size(); i++) {
                walkNode(children.get(i), depth + 1, seenIds, nodeCount, errors, nodePath + ".children");
            }
        }

        // 递归：two-column 的 props.left / props.right（可能是 Map 结构）
        if (isContainer) {
            walkPropNodeList(props.get("left"), depth + 1, seenIds, nodeCount, errors, nodePath + ".props.left");
            walkPropNodeList(props.get("right"), depth + 1, seenIds, nodeCount, errors, nodePath + ".props.right");
        }
    }

    @SuppressWarnings("unchecked")
    private void walkPropNodeList(
            Object value,
            int depth,
            Set<String> seenIds,
            int[] nodeCount,
            List<String> errors,
            String path
    ) {
        if (value == null) {
            return;
        }
        if (!(value instanceof List<?> list)) {
            errors.add(path + ": expected array of nodes");
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            String itemPath = path + "[" + i + "]";
            if (item instanceof DslNode dslNode) {
                walkNode(dslNode, depth, seenIds, nodeCount, errors, path);
            } else if (item instanceof Map<?, ?> map) {
                DslNode converted = mapToNode((Map<String, Object>) map);
                walkNode(converted, depth, seenIds, nodeCount, errors, path);
            } else {
                errors.add(itemPath + ": expected node object");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private DslNode mapToNode(Map<String, Object> map) {
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

    private void validateTypeSpecific(String type, Map<String, Object> props, String path, List<String> errors) {
        switch (type) {
            case "DataTable" -> {
                Object columns = props.get("columns");
                if (!(columns instanceof List<?>)) {
                    errors.add(path + ": DataTable.props.columns must be an array");
                } else {
                    validateIdentifierFields((List<?>) columns, "prop", path + ".columns", errors);
                }
            }
            case "SearchBar" -> {
                Object fields = props.get("fields");
                if (!(fields instanceof List<?>)) {
                    errors.add(path + ": SearchBar.props.fields must be an array");
                } else {
                    validateIdentifierFields((List<?>) fields, "name", path + ".fields", errors);
                }
            }
            case "FormSection" -> {
                Object fields = props.get("fields");
                if (!(fields instanceof List<?>)) {
                    errors.add(path + ": FormSection.props.fields must be an array");
                } else {
                    validateIdentifierFields((List<?>) fields, "name", path + ".fields", errors);
                }
            }
            case "Pagination" -> {
                Object pageSize = props.get("pageSize");
                if (!(pageSize instanceof Number)) {
                    errors.add(path + ": Pagination.props.pageSize must be a number");
                }
            }
            case "TextBlock" -> {
                Object content = props.get("content");
                if (!(content instanceof String)) {
                    errors.add(path + ": TextBlock.props.content must be a string");
                }
            }
            case "Container" -> {
                Object layout = props.get("layout");
                if (!(layout instanceof String) || !CONTAINER_LAYOUTS.contains(layout)) {
                    errors.add(path + ": Container.props.layout must be stack|two-column, got: " + layout);
                }
            }
            case "PageHeader" -> {
                Object title = props.get("title");
                if (!(title instanceof String) || ((String) title).isBlank()) {
                    errors.add(path + ": PageHeader.props.title should be a non-blank string");
                }
            }
            case "ActionBar" -> {
                Object actions = props.get("actions");
                if (!(actions instanceof List<?>)) {
                    errors.add(path + ": ActionBar.props.actions should be an array");
                }
            }
            case "StatCards" -> requireArray(props, "items", path, "StatCards", errors);
            case "Breadcrumb" -> requireArray(props, "items", path, "Breadcrumb", errors);
            case "AlertBanner" -> {
                Object title = props.get("title");
                if (!(title instanceof String) || ((String) title).isBlank()) {
                    errors.add(path + ": AlertBanner.props.title should be a non-blank string");
                }
            }
            case "DescriptionList" -> requireArray(props, "items", path, "DescriptionList", errors);
            case "Tabs" -> requireArray(props, "items", path, "Tabs", errors);
            case "Steps" -> requireArray(props, "items", path, "Steps", errors);
            case "Timeline" -> requireArray(props, "items", path, "Timeline", errors);
            case "TagGroup" -> requireArray(props, "tags", path, "TagGroup", errors);
            case "TreeNav" -> requireArray(props, "data", path, "TreeNav", errors);
            case "EmptyState" -> {
                Object title = props.get("title");
                if (!(title instanceof String)) {
                    errors.add(path + ": EmptyState.props.title should be a string");
                }
            }
            case "ResultBlock" -> {
                Object title = props.get("title");
                if (!(title instanceof String) || ((String) title).isBlank()) {
                    errors.add(path + ": ResultBlock.props.title should be a non-blank string");
                }
            }
            case "ImageBlock" -> {
                Object src = props.get("src");
                if (!(src instanceof String) || ((String) src).isBlank()) {
                    errors.add(path + ": ImageBlock.props.src should be a non-blank string");
                }
            }
            case "Divider" -> {
                // optional content string
            }
            default -> {
                // no-op
            }
        }
    }

    private void requireArray(Map<String, Object> props, String key, String path, String type, List<String> errors) {
        Object val = props.get(key);
        if (!(val instanceof List<?>)) {
            errors.add(path + ": " + type + ".props." + key + " should be an array");
        }
    }

    @SuppressWarnings("unchecked")
    private void validateIdentifierFields(List<?> items, String key, String path, List<String> errors) {
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            if (!(item instanceof Map<?, ?>)) {
                continue;
            }
            Map<String, Object> m = (Map<String, Object>) item;
            Object val = m.get(key);
            if (val == null) {
                continue;
            }
            String s = String.valueOf(val);
            if (!IDENTIFIER.matcher(s).matches()) {
                errors.add(path + "[" + i + "]." + key + " must match identifier pattern, got: " + s);
            }
        }
    }

    private static String safeId(String id) {
        return id == null || id.isBlank() ? "?" : id;
    }
}
