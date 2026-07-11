package com.studio.vuepage.dsl.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 页面 DSL 根模型（version 1）。Jackson 友好的可变 POJO。
 */
public class PageDsl {

    private String version;
    private String pageType;
    private String style;
    private String title;
    private List<DslNode> children = new ArrayList<>();
    private Map<String, Object> meta;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPageType() {
        return pageType;
    }

    public void setPageType(String pageType) {
        this.pageType = pageType;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<DslNode> getChildren() {
        return children;
    }

    public void setChildren(List<DslNode> children) {
        this.children = children != null ? children : new ArrayList<>();
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta != null ? new LinkedHashMap<>(meta) : null;
    }
}
