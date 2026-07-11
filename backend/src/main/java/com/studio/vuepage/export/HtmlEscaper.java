package com.studio.vuepage.export;

/**
 * HTML 上下文转义工具。模板文本节点与属性值必须分别按上下文转义。
 */
public final class HtmlEscaper {

    private HtmlEscaper() {
    }

    /**
     * 转义 HTML 文本内容：&lt; &gt; &amp; &quot;
     */
    public static String escapeText(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 转义 HTML 属性值（双引号属性上下文）。
     */
    public static String escapeAttr(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
