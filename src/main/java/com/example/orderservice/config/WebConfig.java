package com.example.orderservice.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class WebConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public Filter h2ConsoleFrameOptionsFilter() {
        return (request, response, chain) -> {
            chain.doFilter(request, response);
            if (response instanceof HttpServletResponse httpResponse) {
                httpResponse.setHeader("X-Frame-Options", "ALLOWALL");
            }
        };
    }
}
