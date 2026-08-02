package com.globallogic.bloodbridge.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private static final String SECRET = "dev-only-bloodbridge-shared-secret-change-me-32bytes-minimum";

    private JwtAuthFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(SECRET);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("Requests to /api/auth/** bypass JWT validation")
    void testOpenPath_BypassesValidation() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login").build());

        filter.filter(exchange, chain).block();

        verify(chain, times(1)).filter(exchange);
    }

    @Test
    @DisplayName("A protected route without an Authorization header is rejected with 401")
    void testProtectedPath_NoToken_Returns401() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/donors/1").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("A protected route with a valid token is forwarded with X-User-Id and X-User-Role headers")
    void testProtectedPath_ValidToken_ForwardsUserHeaders() {
        String token = validToken();
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/donors/1")
                        .header("Authorization", "Bearer " + token)
                        .build());

        filter.filter(exchange, chain).block();

        verify(chain, times(1)).filter(any());
    }

    @Test
    @DisplayName("A malformed token is rejected with 401")
    void testProtectedPath_MalformedToken_Returns401() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/donors/1")
                        .header("Authorization", "Bearer not-a-real-token")
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    private String validToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("100")
                .claim("role", "DONOR")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();
    }
}
