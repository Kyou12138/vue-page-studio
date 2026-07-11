package com.studio.vuepage.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LangChain4j ChatModel Bean。api-key 为空时使用占位 key，保证应用可启动；
 * 真正调用在 {@link com.studio.vuepage.ai.AiDslService} 中检查配置。
 */
@Configuration
public class LangChain4jConfig {

    @Bean
    public ChatModel chatModel(StudioProperties props) {
        StudioProperties.Llm llm = props.getLlm();
        String apiKey = llm.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = "dummy";
        }
        return OpenAiChatModel.builder()
                .baseUrl(llm.getBaseUrl())
                .apiKey(apiKey)
                .modelName(llm.getModel())
                .temperature(llm.getTemperature())
                .maxTokens(llm.getMaxTokens())
                .timeout(Duration.ofSeconds(Math.max(1, llm.getTimeoutSeconds())))
                .maxRetries(1)
                .build();
    }
}
