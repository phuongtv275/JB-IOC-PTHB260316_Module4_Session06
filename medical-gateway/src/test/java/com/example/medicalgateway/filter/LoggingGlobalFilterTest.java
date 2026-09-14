package com.example.medicalgateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingGlobalFilterTest {

    @InjectMocks
    private LoggingGlobalFilter loggingGlobalFilter;

    @Mock
    private GatewayFilterChain filterChain;

    @Test
    @DisplayName("Filter ghi log và tự động sinh Correlation ID nếu request chưa có")
    void filter_GeneratesCorrelationId_WhenMissing() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/patients?page=0&size=10")
                .remoteAddress(new InetSocketAddress("192.168.1.50", 54321))
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(filterChain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(loggingGlobalFilter.filter(exchange, filterChain))
                .verifyComplete();

        ServerWebExchange capturedExchange = captor.getValue();
        assertNotNull(capturedExchange);

        // Kiểm tra Correlation ID được đính kèm vào request gửi tới downstream
        String reqCorrelationId = capturedExchange.getRequest().getHeaders().getFirst(LoggingGlobalFilter.CORRELATION_ID_HEADER);
        assertNotNull(reqCorrelationId);
        assertTrue(reqCorrelationId.length() > 10);

        // Kiểm tra Correlation ID được đính kèm vào response trả về client
        String resCorrelationId = capturedExchange.getResponse().getHeaders().getFirst(LoggingGlobalFilter.CORRELATION_ID_HEADER);
        assertEquals(reqCorrelationId, resCorrelationId);

        verify(filterChain).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("Filter bảo toàn Correlation ID và đọc Remote IP từ X-Forwarded-For")
    void filter_PreservesExistingCorrelationId_AndReadsXForwardedFor() {
        String existingCid = "existing-corr-id-999";
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/appointments")
                .header(LoggingGlobalFilter.CORRELATION_ID_HEADER, existingCid)
                .header("X-Forwarded-For", "203.0.113.195, 70.41.3.18")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(filterChain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(loggingGlobalFilter.filter(exchange, filterChain))
                .verifyComplete();

        ServerWebExchange capturedExchange = captor.getValue();
        String reqCorrelationId = capturedExchange.getRequest().getHeaders().getFirst(LoggingGlobalFilter.CORRELATION_ID_HEADER);
        assertEquals(existingCid, reqCorrelationId);

        String resCorrelationId = capturedExchange.getResponse().getHeaders().getFirst(LoggingGlobalFilter.CORRELATION_ID_HEADER);
        assertEquals(existingCid, resCorrelationId);
    }

    @Test
    @DisplayName("Kiểm tra thứ tự ưu tiên của filter là HIGHEST_PRECEDENCE")
    void getOrder_ReturnsHighestPrecedence() {
        assertEquals(Ordered.HIGHEST_PRECEDENCE, loggingGlobalFilter.getOrder());
    }
}
