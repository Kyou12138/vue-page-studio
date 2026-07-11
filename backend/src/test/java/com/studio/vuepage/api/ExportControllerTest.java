package com.studio.vuepage.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exportListUser() throws Exception {
        String json = loadClasspath("fixtures/dsl/list-user.json");

        mockMvc.perform(post("/api/export/vue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value(containsString("el-table")));
    }

    private static String loadClasspath(String path) throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ExportControllerTest.class.getClassLoader();
        }
        try (InputStream in = cl.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("classpath resource not found: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
