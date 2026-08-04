package com.globallogic.bloodbridge.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final List<String> OPEN_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login"
    );

    private final SecretKey signingKey;

    public JwtAuthFilter(@Value("${bloodbridge.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        // Browser CORS preflight has no Authorization header — must not return 401.
        if (HttpMethod.OPTIONS.equals(method) || isOpenPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String role = claims.get("role", String.class);

            String requiredRole = requiredRoleFor(method, path);
            if (requiredRole != null && !requiredRole.equals(role)) {
                return forbidden(exchange);
            }

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (JwtException | IllegalArgumentException ex) {
            return unauthorized(exchange);
        }
    }

    private boolean isOpenPath(String path) {
        return OPEN_PATHS.stream().anyMatch(path::equals)
                || path.startsWith("/api/auth/register")
                || path.startsWith("/api/auth/login");
    }

    private String requiredRoleFor(HttpMethod method, String path) {
        if (path.startsWith("/api/analytics/")) {
            return "ADMIN";
        }
        if (path.equals("/api/requests/all") || path.startsWith("/api/requests/all?")) {
            return "ADMIN";
        }
        // Admin CRUD on a specific request (PUT/DELETE /api/requests/{id}) — not cancel/status subpaths.
        if (path.matches("/api/requests/\\d+") && (HttpMethod.PUT.equals(method) || HttpMethod.DELETE.equals(method))) {
            return "ADMIN";
        }
        if (path.startsWith("/api/auth/users") && HttpMethod.DELETE.equals(method)) {
            return "ADMIN";
        }
        if (path.equals("/api/auth/users") || path.startsWith("/api/auth/users?")) {
            // Full user listing is admin-only; Feign service calls bypass the gateway.
            return "ADMIN";
        }
        if (path.equals("/api/inventory") && HttpMethod.GET.equals(method)) {
            return "ADMIN";
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
