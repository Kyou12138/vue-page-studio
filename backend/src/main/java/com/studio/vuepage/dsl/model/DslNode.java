package com.studio.vuepage.dsl.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DSL 树节点。仅 Container 允许 children。
 */
public class DslNode {

    private String id;
    private String type;
    private Map<String, Object> props = new LinkedHashMap<>();
    /** 仅 type === Container 时允许非空 */
    private List<DslNode> children;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getProps() {
        return props;
    }

    public void setProps(Map<String, Object> props) {
        this.props = props != null ? new LinkedHashMap<>(props) : new LinkedHashMap<>();
    }

    public List<DslNode> getChildren() {
        return children;
    }

    public void setChildren(List<DslNode> children) {
        this.children = children;
    }
}
