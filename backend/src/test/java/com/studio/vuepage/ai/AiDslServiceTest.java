package com.studio.vuepage.ai;

import com.studio.vuepage.ai.AiExceptions.ConfigMissingException;
import com.studio.vuepage.ai.AiExceptions.LlmBadJsonException;
import com.studio.vuepage.config.StudioProperties;
import com.studio.vuepage.dsl.DslIdReconciler;
import com.studio.vuepage.dsl.DslJsonSupport;
import com.studio.vuepage.dsl.DslValidator;
import com.studio.vuepage.dsl.model.DslNode;
import com.studio.vuepage.dsl.model.PageDsl;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiDslServiceTest {

    private StudioProperties props;
    private AtomicReference<String> nextResponse;
    private AtomicInteger callCount;
    private ChatModel chatModel;
    private AiDslService service;

    @BeforeEach
    void setUp() {
        props = new StudioProperties();
        props.getLlm().setApiKey("test-key");

        nextResponse = new AtomicReference<>("");
        callCount = new AtomicInteger(0);

        // 真实 default 方法链 → doChat；用桩实现避免 Mockito 与 default 方法纠缠
        chatModel = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                callCount.incrementAndGet();
                String text = nextResponse.get();
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from(text != null ? text : ""))
                        .build();
            }
        };

        service = new AiDslService(
                chatModel,
                props,
                new DslValidator(),
                new DslIdReconciler(),
                new LlmJsonCleaner(),
                new PromptBuilder(),
                DslJsonSupport.mapper()
        );
    }

    @Test
    void generateSuccessFromFixtureJson() {
        PageDsl fixture = DslJsonSupport.readClasspath("fixtures/dsl/list-user.json");
        nextResponse.set(DslJsonSupport.writeJson(fixture));

        PageDsl result = service.generate("用户管理列表", "list", "element-plus");

        assertThat(result.getPageType()).isEqualTo("list");
        assertThat(result.getTitle()).isEqualTo("用户管理");
        assertThat(result.getChildren()).isNotEmpty();
        assertThat(callCount.get()).isEqualTo(1);
        // 所有节点 id 非空
        for (DslNode n : result.getChildren()) {
            assertThat(n.getId()).isNotBlank();
        }
    }

    @Test
    void generateWithMarkdownFence() {
        PageDsl fixture = DslJsonSupport.readClasspath("fixtures/dsl/form-user.json");
        String json = DslJsonSupport.writeJson(fixture);
        nextResponse.set("```json\n" + json + "\n```");

        PageDsl result = service.generate("用户表单", "form", "element-plus");
        assertThat(result.getPageType()).isEqualTo("form");
        assertThat(result.getChildren()).isNotEmpty();
    }

    @Test
    void generateMissingApiKeyThrowsConfigMissing() {
        props.getLlm().setApiKey("");
        assertThatThrownBy(() -> service.generate("x", "list", null))
                .isInstanceOf(ConfigMissingException.class);
        assertThat(callCount.get()).isZero();
    }

    @Test
    void generateInvalidJsonRetriesThenFails() {
        nextResponse.set("not-json-at-all");

        assertThatThrownBy(() -> service.generate("x", "list", "element-plus"))
                .isInstanceOf(LlmBadJsonException.class);
        // 首次 + 修复重试
        assertThat(callCount.get()).isEqualTo(2);
    }

    @Test
    void generateInvalidThenFixedOnRetry() {
        PageDsl fixture = DslJsonSupport.readClasspath("fixtures/dsl/dashboard-min.json");
        String good = DslJsonSupport.writeJson(fixture);
        AtomicInteger n = callCount;
        AtomicReference<String> resp = nextResponse;

        chatModel = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                int c = n.incrementAndGet();
                String text = c == 1 ? "```\nbogus\n```" : good;
                resp.set(text);
                return ChatResponse.builder().aiMessage(AiMessage.from(text)).build();
            }
        };
        service = new AiDslService(chatModel, props);

        PageDsl result = service.generate("看板", "dashboard", "element-plus");
        assertThat(result.getPageType()).isEqualTo("dashboard");
        assertThat(n.get()).isEqualTo(2);
    }

    @Test
    void modifyReconcilesShuffledColumnIds() {
        PageDsl current = DslJsonSupport.readClasspath("fixtures/dsl/list-user.json");

        // 构造“模型返回”：同结构但打乱 column / 节点 id
        PageDsl shuffled = deepCopy(current);
        DslNode table = findByType(shuffled, "DataTable");
        assertThat(table).isNotNull();
        table.setId("DataTable_zzzzzzzz");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = (List<Map<String, Object>>) table.getProps().get("columns");
        List<String> originalColIds = new ArrayList<>();
        for (Map<String, Object> col : columns) {
            originalColIds.add(String.valueOf(col.get("id")));
            col.put("id", "col_shuffled_" + col.get("prop"));
        }
        // 打乱 SearchBar id
        DslNode search = findByType(shuffled, "SearchBar");
        if (search != null) {
            search.setId("SearchBar_newnew01");
        }

        nextResponse.set(DslJsonSupport.writeJson(shuffled));

        PageDsl result = service.modify(current, "把邮箱列改名为电子邮箱");

        // 节点 id 应恢复
        DslNode resultTable = findByType(result, "DataTable");
        assertThat(resultTable).isNotNull();
        assertThat(resultTable.getId()).isEqualTo(findByType(current, "DataTable").getId());

        DslNode resultSearch = findByType(result, "SearchBar");
        if (resultSearch != null) {
            assertThat(resultSearch.getId()).isEqualTo(findByType(current, "SearchBar").getId());
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resultCols =
                (List<Map<String, Object>>) resultTable.getProps().get("columns");
        List<String> resultColIds = resultCols.stream()
                .map(c -> String.valueOf(c.get("id")))
                .toList();
        assertThat(resultColIds).containsExactlyElementsOf(originalColIds);
    }

    @Test
    void ensureIdsFillsMissing() {
        PageDsl dsl = DslJsonSupport.readClasspath("fixtures/dsl/list-user.json");
        DslNode header = findByType(dsl, "PageHeader");
        assertThat(header).isNotNull();
        header.setId(null);

        DslNode table = findByType(dsl, "DataTable");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = (List<Map<String, Object>>) table.getProps().get("columns");
        columns.get(0).remove("id");

        service.ensureIds(dsl);

        assertThat(header.getId()).isNotBlank();
        assertThat(header.getId()).startsWith("PageHeader_");
        assertThat(columns.get(0).get("id")).isNotNull();
        assertThat(String.valueOf(columns.get(0).get("id"))).isNotBlank();
    }

    private static PageDsl deepCopy(PageDsl source) {
        return DslJsonSupport.readJson(DslJsonSupport.writeJson(source));
    }

    private static DslNode findByType(PageDsl dsl, String type) {
        if (dsl.getChildren() == null) {
            return null;
        }
        for (DslNode n : dsl.getChildren()) {
            if (type.equals(n.getType())) {
                return n;
            }
        }
        return null;
    }
}
