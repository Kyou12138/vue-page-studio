package com.studio.vuepage.ai;

/**
 * 清洗大模型输出：剥离 markdown 代码围栏、BOM、首尾空白，尽量得到纯 JSON 文本。
 */
public final class LlmJsonCleaner {

    public String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.strip();
        if (text.isEmpty()) {
            return "";
        }
        // UTF-8 BOM
        if (text.charAt(0) == '\uFEFF') {
            text = text.substring(1).strip();
        }

        text = stripMarkdownFence(text).strip();

        // 若仍夹杂说明文字，尝试截取首个 JSON 对象/数组
        int objStart = text.indexOf('{');
        int arrStart = text.indexOf('[');
        int start = firstNonNegative(objStart, arrStart);
        if (start > 0) {
            text = text.substring(start).strip();
        }

        int end = lastJsonEnd(text);
        if (end >= 0 && end < text.length() - 1) {
            text = text.substring(0, end + 1).strip();
        }

        return text;
    }

    private static String stripMarkdownFence(String text) {
        // ```json ... ``` 或 ``` ... ```
        if (!text.startsWith("```")) {
            return text;
        }
        int firstNl = indexOfNewline(text);
        if (firstNl < 0) {
            // 单行 fence：```json{...}```
            String withoutOpen = text.substring(3);
            int close = withoutOpen.lastIndexOf("```");
            if (close >= 0) {
                withoutOpen = withoutOpen.substring(0, close);
            }
            // 去掉可选语言标签
            withoutOpen = withoutOpen.replaceFirst("(?i)^json\\s*", "");
            return withoutOpen.strip();
        }
        String body = text.substring(firstNl + 1);
        int close = body.lastIndexOf("```");
        if (close >= 0) {
            body = body.substring(0, close);
        }
        return body.strip();
    }

    private static int indexOfNewline(String s) {
        int n = s.indexOf('\n');
        int r = s.indexOf('\r');
        return firstNonNegative(n, r);
    }

    private static int firstNonNegative(int a, int b) {
        if (a < 0) {
            return b;
        }
        if (b < 0) {
            return a;
        }
        return Math.min(a, b);
    }

    /**
     * 从末尾找到匹配的 JSON 闭合位置（} 或 ]）。
     * 简单策略：取最后一个 } 与最后一个 ] 中更靠后且合理的那个。
     */
    private static int lastJsonEnd(String text) {
        if (text.isEmpty()) {
            return -1;
        }
        char open = text.charAt(0);
        if (open == '{') {
            return text.lastIndexOf('}');
        }
        if (open == '[') {
            return text.lastIndexOf(']');
        }
        return -1;
    }
}
