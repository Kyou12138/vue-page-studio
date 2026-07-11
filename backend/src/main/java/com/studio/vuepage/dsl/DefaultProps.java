package com.studio.vuepage.dsl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 各组件类型的默认 props，供画布投放 / 骨架生成使用。
 */
public final class DefaultProps {

    private DefaultProps() {
    }

    public static Map<String, Object> forType(String type) {
        if (type == null) {
            return new LinkedHashMap<>();
        }
        return switch (type) {
            case "PageHeader" -> mapOf("title", "页面标题", "actions", new ArrayList<>());
            case "Breadcrumb" -> mapOf("items", sampleItems(
                    item("bc1", "label", "首页"),
                    item("bc2", "label", "当前页")
            ));
            case "AlertBanner" -> mapOf(
                    "title", "提示信息",
                    "description", "这里是补充说明",
                    "type", "info",
                    "closable", false
            );
            case "SearchBar" -> mapOf("fields", sampleItems(
                    field("f1", "keyword", "关键词", "input")
            ));
            case "DataTable" -> mapOf(
                    "columns", sampleItems(
                            col("c1", "name", "名称"),
                            col("c2", "status", "状态")
                    ),
                    "rowActions", new ArrayList<>(),
                    "dataSource", "mock"
            );
            case "Pagination" -> mapOf("pageSize", 10);
            case "FormSection" -> mapOf(
                    "fields", sampleItems(field("f1", "name", "名称", "input")),
                    "columns", 1
            );
            case "DescriptionList" -> mapOf(
                    "column", 2,
                    "items", sampleItems(
                            desc("d1", "状态", "已启用"),
                            desc("d2", "创建时间", "2026-01-01")
                    )
            );
            case "ActionBar" -> mapOf("actions", sampleItems(
                    action("a1", "提交", "primary"),
                    action("a2", "取消", "default")
            ));
            case "StatCards" -> mapOf("items", sampleItems(
                    stat("s1", "今日访问", "1,280", ""),
                    stat("s2", "转化率", "12.5", "%")
            ));
            case "Tabs" -> mapOf(
                    "active", "tab1",
                    "items", sampleItems(
                            tab("t1", "tab1", "概览"),
                            tab("t2", "tab2", "明细")
                    )
            );
            case "Steps" -> mapOf(
                    "active", 1,
                    "items", sampleItems(
                            step("st1", "填写信息", "基本资料"),
                            step("st2", "确认", "核对内容"),
                            step("st3", "完成", "提交成功")
                    )
            );
            case "Timeline" -> mapOf("items", sampleItems(
                    timeline("tl1", "创建订单", "10:00"),
                    timeline("tl2", "支付完成", "10:15")
            ));
            case "TagGroup" -> mapOf("tags", sampleItems(
                    tag("tg1", "标签A", ""),
                    tag("tg2", "标签B", "success")
            ));
            case "TreeNav" -> mapOf("data", sampleTree());
            case "EmptyState" -> mapOf("title", "暂无数据", "description", "可以新建一条记录试试");
            case "ResultBlock" -> mapOf(
                    "status", "success",
                    "title", "操作成功",
                    "subTitle", "页面将在稍后跳转"
            );
            case "ImageBlock" -> mapOf(
                    "src", "https://via.placeholder.com/640x200?text=Image",
                    "alt", "示意图片",
                    "fit", "cover"
            );
            case "Divider" -> mapOf("content", "", "contentPosition", "center");
            case "TextBlock" -> mapOf("content", "说明文本", "variant", "body");
            case "Container" -> mapOf("layout", "stack");
            default -> new LinkedHashMap<>();
        };
    }

    public static List<String> supportedTypes() {
        return List.of(
                "PageHeader", "Breadcrumb", "AlertBanner", "SearchBar", "DataTable",
                "Pagination", "FormSection", "DescriptionList", "ActionBar", "StatCards",
                "Tabs", "Steps", "Timeline", "TagGroup", "TreeNav",
                "EmptyState", "ResultBlock", "ImageBlock", "Divider", "TextBlock", "Container"
        );
    }

    public static Map<String, Object> copy(Map<String, Object> source) {
        if (source == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(source);
    }

    private static List<Map<String, Object>> sampleItems(Map<String, Object>... items) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> item : items) {
            list.add(item);
        }
        return list;
    }

    private static List<Map<String, Object>> sampleTree() {
        Map<String, Object> child = item("n1-1", "label", "子项 A");
        Map<String, Object> n1 = item("n1", "label", "目录一");
        n1.put("children", List.of(child));
        Map<String, Object> n2 = item("n2", "label", "目录二");
        return sampleItems(n1, n2);
    }

    private static Map<String, Object> item(String id, String k, Object v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put(k, v);
        return m;
    }

    private static Map<String, Object> field(String id, String name, String label, String control) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("label", label);
        m.put("control", control);
        return m;
    }

    private static Map<String, Object> col(String id, String prop, String label) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("prop", prop);
        m.put("label", label);
        return m;
    }

    private static Map<String, Object> desc(String id, String label, String value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("label", label);
        m.put("value", value);
        return m;
    }

    private static Map<String, Object> action(String id, String label, String type) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("label", label);
        m.put("type", type);
        return m;
    }

    private static Map<String, Object> stat(String id, String label, String value, String unit) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("label", label);
        m.put("value", value);
        m.put("unit", unit);
        return m;
    }

    private static Map<String, Object> tab(String id, String name, String label) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("label", label);
        return m;
    }

    private static Map<String, Object> step(String id, String title, String description) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("title", title);
        m.put("description", description);
        return m;
    }

    private static Map<String, Object> timeline(String id, String content, String timestamp) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("content", content);
        m.put("timestamp", timestamp);
        return m;
    }

    private static Map<String, Object> tag(String id, String label, String type) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("label", label);
        m.put("type", type);
        return m;
    }

    private static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }
}
