package com.studio.vuepage.ai;

import com.studio.vuepage.dsl.DefaultProps;
import com.studio.vuepage.dsl.DslJsonSupport;
import com.studio.vuepage.dsl.model.PageDsl;

/**
 * 构造 generate / modify 的 system 与 user 提示词。
 */
public final class PromptBuilder {

    private static final String WHITELIST_TYPES = String.join(", ", DefaultProps.supportedTypes());

    private static final String SYSTEM_COMMON = """
            You are a PageDSL generator for a Vue mid-admin page studio.
            Output ONLY valid pure JSON for PageDSL. No markdown fences, no comments, no explanation.

            Schema rules:
            - version must be the string "1"
            - pageType: list | form | detail | dashboard
            - style: element-plus | plain
            - title: non-blank string
            - children: ordered array of nodes
            - Each node: { "id", "type", "props", "children?" }
            - id format: {type}_{8hex} e.g. PageHeader_a1b2c3d4 (unique across the page)
            - type whitelist ONLY: %s
            - Only Container may have children; Container.props.layout is stack | two-column
            - two-column may use props.left / props.right as node arrays
            - DataTable.props.columns: array of {id, prop, label}; prop must be identifier [a-zA-Z_][a-zA-Z0-9_]*
            - SearchBar/FormSection.props.fields: array of {id, name, label, control?}; name must be identifier
            - Pagination.props.pageSize: number
            - TextBlock.props.content: string
            - PageHeader.props.title: non-blank string; optional actions array
            - ActionBar.props.actions: array
            - StatCards.props.items: array
            - Prefer total nodes ≤ 50 and depth ≤ 3
            - Prefer realistic Chinese labels for mid-admin UIs when user writes Chinese

            Minimal example:
            {"version":"1","pageType":"list","style":"element-plus","title":"用户管理","children":[{"id":"PageHeader_a1b2c3d4","type":"PageHeader","props":{"title":"用户管理","actions":[]}},{"id":"DataTable_i9j0k1l2","type":"DataTable","props":{"columns":[{"id":"c_name","prop":"name","label":"姓名"}],"dataSource":"mock"}},{"id":"Pagination_m3n4o5p6","type":"Pagination","props":{"pageSize":10}}]}
            """.formatted(WHITELIST_TYPES);

    private static final String SYSTEM_MODIFY_EXTRA = """

            Modify mode:
            - Return the FULL updated PageDSL JSON (not a patch).
            - Keep ids and props of nodes not mentioned in the instruction whenever possible.
            - Prefer preserving column/field ids when only labels or minor props change.
            """;

    public String systemGenerate() {
        return SYSTEM_COMMON;
    }

    public String systemModify() {
        return SYSTEM_COMMON + SYSTEM_MODIFY_EXTRA;
    }

    public String userGenerate(String description, String pageType, String style) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate a complete PageDSL JSON for the following page description.\n");
        sb.append("Description:\n").append(nullToEmpty(description)).append('\n');
        if (pageType != null && !pageType.isBlank()) {
            sb.append("Preferred pageType: ").append(pageType.trim()).append('\n');
        }
        if (style != null && !style.isBlank()) {
            sb.append("Preferred style: ").append(style.trim()).append('\n');
        } else {
            sb.append("Preferred style: element-plus\n");
        }
        sb.append("Return pure JSON only.");
        return sb.toString();
    }

    public String userModify(PageDsl current, String instruction) {
        String dslJson;
        try {
            dslJson = DslJsonSupport.writeJson(current);
        } catch (Exception e) {
            dslJson = "{}";
        }
        return """
                Modify the following PageDSL according to the instruction.
                Keep unchanged nodes' ids and props when possible.
                Return the full updated PageDSL as pure JSON only.

                Instruction:
                %s

                Current PageDSL:
                %s
                """.formatted(nullToEmpty(instruction), dslJson);
    }

    public String userFix(String previousOutput, java.util.List<String> errors) {
        String errText = errors == null || errors.isEmpty()
                ? "unknown validation/parse error"
                : String.join("; ", errors);
        return """
                Return fixed valid JSON only (PageDSL). No markdown. Fix these issues:
                %s

                Previous output:
                %s
                """.formatted(errText, nullToEmpty(previousOutput));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
