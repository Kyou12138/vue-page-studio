package com.studio.vuepage.api;

import com.studio.vuepage.ai.AiDslService;
import com.studio.vuepage.ai.AiExceptions.ConfigMissingException;
import com.studio.vuepage.dsl.DslJsonSupport;
import com.studio.vuepage.dsl.model.PageDsl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiDslService aiDslService;

    @Test
    void generateReturnsPageDsl() throws Exception {
        PageDsl fixture = DslJsonSupport.readClasspath("fixtures/dsl/list-user.json");
        when(aiDslService.generate(eq("用户列表"), eq("list"), eq("element-plus")))
                .thenReturn(fixture);

        mockMvc.perform(post("/api/ai/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"用户列表","pageType":"list","style":"element-plus"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pageType").value("list"))
                .andExpect(jsonPath("$.data.title").value("用户管理"));
    }

    @Test
    void generateConfigMissingMapsTo503() throws Exception {
        when(aiDslService.generate(anyString(), any(), any()))
                .thenThrow(new ConfigMissingException("no key"));

        mockMvc.perform(post("/api/ai/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"x\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CONFIG_MISSING"));
    }

    @Test
    void modifyReturnsPageDsl() throws Exception {
        PageDsl fixture = DslJsonSupport.readClasspath("fixtures/dsl/list-user.json");
        when(aiDslService.modify(any(PageDsl.class), eq("加一列手机号")))
                .thenReturn(fixture);

        String body = """
                {
                  "dsl": %s,
                  "instruction": "加一列手机号"
                }
                """.formatted(DslJsonSupport.writeJson(fixture));

        mockMvc.perform(post("/api/ai/modify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pageType").value("list"));
    }

    @Test
    void generateMissingDescription400() throws Exception {
        mockMvc.perform(post("/api/ai/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
