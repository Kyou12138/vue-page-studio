package com.studio.vuepage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studio.vuepage.api.ApiResponse;
import com.studio.vuepage.dsl.DslJsonSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 仅作用于 {@code /api/ai/*}：按 IP 分钟级限流 + 可选请求体大小检查。
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;

    private final StudioProperties props;
    private final ObjectMapper mapper;
    /** IP → 请求时间戳队列（滑动窗口） */
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(StudioProperties props) {
        this(props, DslJsonSupport.mapper());
    }

    public RateLimitFilter(StudioProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper != null ? mapper : DslJsonSupport.mapper();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        // 兼容 context-path
        String servletPath = request.getServletPath();
        String p = servletPath != null && !servletPath.isEmpty() ? servletPath : path;
        return !p.startsWith("/api/ai");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long maxBody = props.getMaxBodyBytes();
        if (maxBody > 0) {
            long contentLength = request.getContentLengthLong();
            if (contentLength > maxBody) {
                writeJson(response, HttpStatus.PAYLOAD_TOO_LARGE,
                        ApiResponse.fail("BODY_TOO_LARGE",
                                "Request body exceeds limit of " + maxBody + " bytes", null));
                return;
            }
        }

        int limit = props.getRateLimit().getAiPerIpPerMinute();
        if (limit > 0) {
            String ip = clientIp(request);
            long now = System.currentTimeMillis();
            ConcurrentLinkedDeque<Long> deque = windows.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());
            synchronized (deque) {
                prune(deque, now);
                if (deque.size() >= limit) {
                    writeJson(response, HttpStatus.TOO_MANY_REQUESTS,
                            ApiResponse.fail("RATE_LIMITED",
                                    "AI rate limit exceeded: " + limit + " requests per minute per IP", null));
                    return;
                }
                deque.addLast(now);
            }
            // 偶尔清理空窗口，避免无限增长
            if (windows.size() > 10_000) {
                cleanupStale(now);
            }
        }

        filterChain.doFilter(request, response);
    }

    private static void prune(ConcurrentLinkedDeque<Long> deque, long now) {
        long threshold = now - WINDOW_MS;
        while (true) {
            Long head = deque.peekFirst();
            if (head == null || head >= threshold) {
                break;
            }
            deque.pollFirst();
        }
    }

    private void cleanupStale(long now) {
        long threshold = now - WINDOW_MS;
        Iterator<Map.Entry<String, ConcurrentLinkedDeque<Long>>> it = windows.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ConcurrentLinkedDeque<Long>> e = it.next();
            ConcurrentLinkedDeque<Long> q = e.getValue();
            synchronized (q) {
                prune(q, now);
                if (q.isEmpty() || (q.peekLast() != null && q.peekLast() < threshold)) {
                    it.remove();
                }
            }
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String remote = request.getRemoteAddr();
        return remote != null ? remote : "unknown";
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, ApiResponse<?> body)
            throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), body);
    }
}
