package com.example.medicalgateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * GlobalFilter ghi log cho mọi request HTTP đi qua API Gateway.
 *
 * Nhiệm vụ chính:
 * 1. Ghi nhận thông tin request:
 *    - Địa chỉ IP máy khách (Remote Address), có hỗ trợ đọc từ header X-Forwarded-For khi qua proxy/load balancer.
 *    - Phương thức HTTP (GET, POST, PUT, DELETE...).
 *    - Đường dẫn (Path) và Query String mà khách hàng đang truy cập.
 * 2. Đảm bảo tính toàn vẹn tracing bằng Correlation ID:
 *    - Nhận diện hoặc sinh mới Correlation ID (UUID) nếu request chưa có.
 *    - Đính kèm X-Correlation-Id vào request gửi tới downstream microservices (patient-service, doctor-service, appointment-service).
 *    - Đính kèm X-Correlation-Id vào response trả về cho client.
 * 3. Ghi log sau khi hoàn thành request (Post-filter reactive flow):
 *    - Ghi nhận mã trạng thái HTTP (Status Code).
 *    - Đo lường chính xác thời gian xử lý toàn trình (Duration tính bằng ms).
 */
@Slf4j
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();

        // 1. Trích xuất hoặc khởi tạo Correlation ID
        String correlationId = extractOrCreateCorrelationId(request);

        // 2. Trích xuất Địa chỉ IP máy khách (Remote Address)
        String clientIp = extractClientIp(request);

        // 3. Trích xuất Phương thức HTTP
        String httpMethod = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";

        // 4. Trích xuất Đường dẫn (Path)
        String path = request.getURI().getRawPath();
        String query = request.getURI().getRawQuery();
        String fullPath = StringUtils.hasText(query) ? path + "?" + query : path;

        // Ghi log request đầu vào theo đúng yêu cầu
        log.info("[GATEWAY-REQUEST] [Correlation Id:{}] Client IP: {} | Method: {} | Path: {}",
                correlationId, clientIp, httpMethod, fullPath);

        // 5. Lan truyền Correlation ID sang downstream services và trả về cho client
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        mutatedExchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);

        // 6. Xử lý chuỗi filter và ghi log khi request hoàn tất (Non-blocking Reactive)
        return chain.filter(mutatedExchange)
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    HttpStatusCode statusCode = mutatedExchange.getResponse().getStatusCode();
                    int statusValue = statusCode != null ? statusCode.value() : 0;

                    log.info("[GATEWAY-RESPONSE] [Correlation ID:{}] Client IP: {} | Method: {} | Path: {} | Status: {} | Duration: {}ms | Signal: {}",
                            correlationId, clientIp, httpMethod, fullPath, statusValue, duration, signalType);
                });
    }

    /**
     * Xác định thứ tự ưu tiên thực thi của Filter trong chuỗi Gateway Filters.
     * Đặt HIGHEST_PRECEDENCE để chạy đầu tiên, đảm bảo mọi request đều được gán Correlation ID và log sớm nhất.
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * Logic trích xuất hoặc sinh mới Correlation ID.
     */
    private String extractOrCreateCorrelationId(ServerHttpRequest request) {
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        }
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    /**
     * Logic trích xuất IP client an toàn:
     * - Ưu tiên đọc từ 'X-Forwarded-For' (lấy IP đầu tiên trong danh sách nếu có nhiều proxy/CDN).
     * - Kế tiếp kiểm tra 'X-Real-IP'.
     * - Cuối cùng fallback về remoteAddress trực tiếp của kết nối socket.
     */
    private String extractClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            // Trường hợp có chuỗi proxy: client, proxy1, proxy2... -> lấy IP đầu tiên
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp.trim();
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return "UNKNOWN";
    }
}
