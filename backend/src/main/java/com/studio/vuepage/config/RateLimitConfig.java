package com.studio.vuepage.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 注册 AI 接口限流过滤器。
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public FilterRegistrationBean<RateLimitFilter> aiRateLimitFilter(StudioProperties props) {
        FilterRegistrationBean<RateLimitFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RateLimitFilter(props));
        bean.addUrlPatterns("/api/ai/*");
        bean.setName("aiRateLimitFilter");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return bean;
    }
}
