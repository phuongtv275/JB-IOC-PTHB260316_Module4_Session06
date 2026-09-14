package com.example.appointmentservice.config;

import com.example.appointmentservice.filter.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * Cấu hình RestTemplate có gắn @LoadBalanced để gọi các microservice khác thông qua Eureka Server.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Khởi tạo RestTemplate có khả năng phân giải Service Name (Service Discovery) và cân bằng tải (Client-side Load Balancing).
     * Tự động truyền tiếp header X-Correlation-Id sang các service khác (distributed tracing).
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Interceptor tự động gán header X-Correlation-Id từ MDC hiện tại vào mọi request gửi đi
        ClientHttpRequestInterceptor correlationInterceptor = (request, body, execution) -> {
            String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
            if (correlationId != null && !correlationId.isBlank()) {
                request.getHeaders().add(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
            }
            return execution.execute(request, body);
        };

        restTemplate.setInterceptors(Collections.singletonList(correlationInterceptor));
        return restTemplate;
    }
}
