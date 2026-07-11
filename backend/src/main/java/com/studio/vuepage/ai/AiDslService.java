package com.studio.vuepage.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studio.vuepage.ai.AiExceptions.ConfigMissingException;
import com.studio.vuepage.ai.AiExceptions.LlmBadJsonException;
import com.studio.vuepage.ai.AiExceptions.LlmTimeoutException;
import com.studio.vuepage.config.StudioProperties;
import com.studio.vuepage.dsl.DslIdReconciler;
import com.studio.vuepage.dsl.DslJsonSupport;
import com.studio.vuepage.dsl.DslValidator;
import com.studio.vuepage.dsl.model.DslNode;
import com.studio.vuepage.dsl.model.PageDsl;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.TimeoutException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于 LangChain4j 的 PageDSL 生成与修改服务。
 */
@Service
public class AiDslService {

    private static final Logger log = LoggerFactory.getLogger(AiDslService.class);

    private static final String[] ARRAY_PROP_KEYS = {
            "columns", "fields", "items", "actions", "rowActions"
    };
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final ChatModel chatModel;
    private final StudioProperties props;
    private final DslValidator validator;
    private final DslIdReconciler reconciler;
    private final LlmJsonCleaner cleaner;
    private final PromptBuilder prompts;
    private final ObjectMapper mapper;

    @Autowired
    public AiDslService(ChatModel chatModel, StudioProperties props) {
        this(chatModel, props, new DslValidator(), new DslIdReconciler(),
                new LlmJsonCleaner(), new PromptBuilder(), DslJsonSupport.mapper());
    }

    public AiDslService(
            ChatModel chatModel,
            StudioProperties props,
            DslValidator validator,
            DslIdReconciler reconciler,
            LlmJsonCleaner cleaner,
            PromptBuilder prompts,
            ObjectMapper mapper
    ) {
        this.chatModel = chatModel;
        this.props = props;
        this.validator = validator != null ? validator : new DslValidator();
        this.reconciler = reconciler != null ? reconciler : new DslIdReconciler();
        this.cleaner = cleaner != null ? cleaner : new LlmJsonCleaner();
        this.prompts = prompts != null ? prompts : new PromptBuilder();
        this.mapper = mapper != null ? mapper : DslJsonSupport.mapper();
    }

    public PageDsl generate(String description, String pageType, String style) {
        requireApiKey();
        String system = prompts.systemGenerate();
        String user = prompts.userGenerate(description, pageType, style);
        PageDsl parsed = callParseValidateWithRetry(system, user);
        ensureIds(parsed);
        List<String> errors = validator.validate(parsed);
        if (!errors.isEmpty()) {
            throw new LlmBadJsonException("Generated DSL failed validation after ensureIds", errors);
        }
        return parsed;
    }

    public PageDsl modify(PageDsl current, String instruction) {
        requireApiKey();
        if (current == null) {
            throw new LlmBadJsonException("current dsl is null");
        }
        String system = prompts.systemModify();
        String user = prompts.userModify(current, instruction);
        PageDsl parsed = callParseValidateWithRetry(system, user);
        ensureIds(parsed);
        PageDsl reconciled = reconciler.reconcile(current, parsed);
        List<String> errors = validator.validate(reconciled);
        if (!errors.isEmpty()) {
            throw new LlmBadJsonException("Modified DSL failed validation after reconcile", errors);
        }
        return reconciled;
    }

    private void requireApiKey() {
        String key = props.getLlm().getApiKey();
        if (key == null || key.isBlank()) {
            throw new ConfigMissingException(
                    "LLM API key is not configured. Set STUDIO_LLM_API_KEY or studio.llm.api-key.");
        }
    }

    private PageDsl callParseValidateWithRetry(String system, String user) {
        String raw = chat(system, user);
        String cleaned = cleaner.clean(raw);
        ParseResult first = tryParseAndValidate(cleaned);
        if (first.dsl != null && first.errors.isEmpty()) {
            return first.dsl;
        }

        List<String> err = new ArrayList<>();
        if (first.parseError != null) {
            err.add(first.parseError);
        }
        err.addAll(first.errors);

        String fixUser = prompts.userFix(cleaned, err);
        String raw2 = chat(system, fixUser);
        String cleaned2 = cleaner.clean(raw2);
        ParseResult second = tryParseAndValidate(cleaned2);
        if (second.dsl != null && second.errors.isEmpty()) {
            return second.dsl;
        }

        List<String> details = new ArrayList<>();
        if (second.parseError != null) {
            details.add(second.parseError);
        }
        details.addAll(second.errors);
        if (details.isEmpty()) {
            details.add("empty or invalid model output");
        }
        throw new LlmBadJsonException("Failed to parse/validate model JSON after retry", details);
    }

    private ParseResult tryParseAndValidate(String cleaned) {
        if (cleaned == null || cleaned.isBlank()) {
            return ParseResult.fail("empty model output after clean", List.of());
        }
        try {
            PageDsl dsl = mapper.readValue(cleaned, PageDsl.class);
            List<String> errors = validator.validate(dsl);
            if (errors.isEmpty()) {
                return ParseResult.ok(dsl);
            }
            return ParseResult.invalid(dsl, errors);
        } catch (Exception e) {
            log.debug("Failed to parse LLM JSON: {}", e.toString());
            return ParseResult.fail("JSON parse error: " + e.getMessage(), List.of());
        }
    }

    private String chat(String system, String user) {
        try {
            ChatResponse response = chatModel.chat(
                    SystemMessage.from(system),
                    UserMessage.from(user)
            );
            if (response == null || response.aiMessage() == null) {
                return "";
            }
            String text = response.aiMessage().text();
            return text != null ? text : "";
        } catch (TimeoutException e) {
            throw new LlmTimeoutException("LLM request timed out", e);
        } catch (RuntimeException e) {
            // 部分 HTTP 客户端会包装超时
            if (isTimeout(e)) {
                throw new LlmTimeoutException("LLM request timed out", e);
            }
            throw e;
        }
    }

    private static boolean isTimeout(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            if (cur instanceof TimeoutException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Timeout") || name.contains("SocketTimeout")) {
                return true;
            }
            String msg = cur.getMessage();
            if (msg != null && msg.toLowerCase().contains("timeout")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    /**
     * 为缺失的节点 / 数组项 id 生成 {@code {type}_{8hex}}。
     */
    void ensureIds(PageDsl dsl) {
        if (dsl == null || dsl.getChildren() == null) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (DslNode child : dsl.getChildren()) {
            ensureNodeIds(child, seen);
        }
    }

    @SuppressWarnings("unchecked")
    private void ensureNodeIds(DslNode node, Set<String> seen) {
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
            ensureSideIds(props.get("left"), seen);
            ensureSideIds(props.get("right"), seen);
        }

        if (node.getChildren() != null) {
            for (DslNode c : node.getChildren()) {
                ensureNodeIds(c, seen);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void ensureSideIds(Object raw, Set<String> seen) {
        if (!(raw instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            if (item instanceof DslNode n) {
                ensureNodeIds(n, seen);
            } else if (item instanceof Map<?, ?> m) {
                Map<String, Object> map = (Map<String, Object>) m;
                String type = str(map.get("type"));
                if (type == null || type.isBlank()) {
                    type = "Node";
                }
                map.put("id", uniqueId(str(map.get("id")), type, seen));
                Object children = map.get("children");
                if (children instanceof List<?> cl) {
                    for (Object c : cl) {
                        if (c instanceof DslNode cn) {
                            ensureNodeIds(cn, seen);
                        } else if (c instanceof Map<?, ?> cm) {
                            ensureSideIds(List.of(cm), seen);
                        }
                    }
                }
                Object nestedProps = map.get("props");
                if (nestedProps instanceof Map<?, ?> pm) {
                    Map<String, Object> props = (Map<String, Object>) pm;
                    ensureSideIds(props.get("left"), seen);
                    ensureSideIds(props.get("right"), seen);
                    for (String key : ARRAY_PROP_KEYS) {
                        Object val = props.get(key);
                        if (!(val instanceof List<?> arr)) {
                            continue;
                        }
                        for (Object arrItem : arr) {
                            if (arrItem instanceof Map<?, ?> am) {
                                Map<String, Object> itemMap = (Map<String, Object>) am;
                                itemMap.put("id", uniqueId(str(itemMap.get("id")), guessItemType(key), seen));
                            }
                        }
                    }
                }
            }
        }
    }

    private String uniqueId(String current, String type, Set<String> seen) {
        String id = current;
        if (id == null || id.isBlank()) {
            id = newId(type);
        }
        if (seen.add(id)) {
            return id;
        }
        String regenerated;
        do {
            regenerated = newId(type);
        } while (!seen.add(regenerated));
        return regenerated;
    }

    static String newId(String type) {
        String t = sanitizeType(type);
        return t + "_" + randomHex8();
    }

    private static String sanitizeType(String type) {
        if (type == null || type.isBlank()) {
            return "Node";
        }
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

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private record ParseResult(PageDsl dsl, List<String> errors, String parseError) {
        static ParseResult ok(PageDsl dsl) {
            return new ParseResult(dsl, List.of(), null);
        }

        static ParseResult invalid(PageDsl dsl, List<String> errors) {
            return new ParseResult(dsl, errors != null ? errors : List.of(), null);
        }

        static ParseResult fail(String parseError, List<String> errors) {
            return new ParseResult(null, errors != null ? errors : List.of(), parseError);
        }
    }
}
