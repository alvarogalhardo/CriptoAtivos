package com.criptoativos.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A fixed-window limit on the authentication endpoints, so password and TOTP guessing is not free.
 *
 * <p>Deliberately in-process and dependency-free: it is the right size for a single instance.
 * Behind a load balancer, or across replicas, this needs to move to shared state such as Redis —
 * noted in the README rather than pretended otherwise.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final boolean enabled;

    public RateLimitFilter(
            @Value("${app.security.rate-limit.auth-requests-per-minute:20}") int maxRequests,
            @Value("${app.security.rate-limit.enabled:true}") boolean enabled) {
        this.maxRequests = maxRequests;
        this.enabled = enabled;
    }

    private record Window(Instant startedAt, AtomicInteger count) {}

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !request.getRequestURI().startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String key = request.getRemoteAddr();
        Instant now = Instant.now();

        Window window =
                windows.compute(
                        key,
                        (ignored, existing) ->
                                existing == null || existing.startedAt().plus(WINDOW).isBefore(now)
                                        ? new Window(now, new AtomicInteger())
                                        : existing);

        if (window.count().incrementAndGet() > maxRequests) {
            log.warn("Rate limit exceeded for {} on {}", key, request.getRequestURI());
            writeTooManyRequests(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private static void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
        response.getWriter()
                .write(
                        """
                        {"type":"https://criptoativos.dev/errors/RateLimitExceeded",\
                        "title":"Too many requests",\
                        "status":429,\
                        "detail":"Too many authentication attempts. Try again shortly."}\
                        """);
    }

    /** Prevents unbounded growth if a process is long-lived and sees many distinct addresses. */
    public void evictExpired() {
        Instant cutoff = Instant.now().minus(WINDOW);
        windows.entrySet().removeIf(entry -> entry.getValue().startedAt().isBefore(cutoff));
    }
}
