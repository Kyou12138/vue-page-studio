package com.studio.vuepage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "studio")
public class StudioProperties {

    private Llm llm = new Llm();
    private String corsAllowedOrigins =
            "http://localhost:3000,http://127.0.0.1:3000,http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173";
    private RateLimit rateLimit = new RateLimit();
    private long maxBodyBytes = 262144L;

    public Llm getLlm() {
        return llm;
    }

    public void setLlm(Llm llm) {
        this.llm = llm;
    }

    public String getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(String corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public long getMaxBodyBytes() {
        return maxBodyBytes;
    }

    public void setMaxBodyBytes(long maxBodyBytes) {
        this.maxBodyBytes = maxBodyBytes;
    }

    public static class Llm {
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey = "";
        private String model = "gpt-4o-mini";
        private double temperature = 0.2;
        private int maxTokens = 8192;
        private int timeoutSeconds = 120;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public static class RateLimit {
        private int aiPerIpPerMinute = 10;

        public int getAiPerIpPerMinute() {
            return aiPerIpPerMinute;
        }

        public void setAiPerIpPerMinute(int aiPerIpPerMinute) {
            this.aiPerIpPerMinute = aiPerIpPerMinute;
        }
    }
}
