package com.example.patientservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter bắt và thiết lập Correlation ID cho từng request HTTP.
 * 
 * Logic hoạt động:
 * 1. Nhận Correlation ID từ HTTP header 'X-Correlation-Id' (hoặc 'X-Request-Id') nếu upstream gửi tới.
 * 2. Nếu không có, tự sinh một chuỗi UUID ngẫu nhiên để định danh duy nhất request này.
 * 3. Đưa correlationId vào SLF4J MDC (Mapped Diagnostic Context) để mọi dòng log trong suốt
 *    vòng đời xử lý request trên thread hiện tại đều tự động mang theo mã định danh này.
 * 4. Đính kèm lại vào header phản hồi cho client để dễ dàng tracing khi gặp sự cố.
 * 5. Đo thời gian xử lý và xóa MDC trong khối finally nhằm tránh rò rỉ dữ liệu giữa các luồng
 *    trong ThreadPool (ThreadLocal leak).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String CORRELATION_ID_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // 1. Trích xuất hoặc sinh mới Correlation ID
        String correlationId = extractCorrelationId(request);

        // 2. Gắn vào MDC và response header
        MDC.put(CORRELATION_ID_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullPath = queryString != null ? uri + "?" + queryString : uri;

        log.info("[HTTP-IN] {} {} | Client IP: {}", method, fullPath, request.getRemoteAddr());

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("[HTTP-OUT] {} {} | Status: {} | Duration: {}ms", method, fullPath, response.getStatus(), duration);
            // Dọn dẹp MDC để ngăn ngừa memory leak và lây lan dữ liệu sang request khác dùng chung thread pool
            MDC.remove(CORRELATION_ID_KEY);
        }
    }

    private String extractCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = request.getHeader(REQUEST_ID_HEADER);
        }
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }
}
