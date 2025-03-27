package blog.collection.gateway_service.filter;
import blog.collection.gateway_service.dto.BaseResponse;
import blog.collection.gateway_service.dto.ErrorResponse;
import org.springframework.web.server.ServerWebExchange;
import blog.collection.gateway_service.security.BlackListToken;
import blog.collection.gateway_service.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AbstractGatewayFilterFactory.NameConfig> {
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private BlackListToken blackListToken;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthFilter() {
        super(NameConfig.class);
    }

    @Override
    public GatewayFilter apply(NameConfig config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().toString();
            if (path.startsWith("/auth/login") ||
                    path.startsWith("/auth/logout") ||
                    path.startsWith("/auth/sign-up") ||
                    path.startsWith("/auth/verify-email") ||
                    path.startsWith("/auth/success") ||
                    path.startsWith("/auth/failure") ||
                    path.startsWith("/auth/reset/verify") ||
                    path.startsWith("/auth/reset-password") ||
                    path.startsWith("/login/oauth2")) {
                return chain.filter(exchange);
            }

            try {
                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED, path);
                }

                String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return onError(exchange, "Invalid Authorization header", HttpStatus.UNAUTHORIZED, path);
                }

                String token = authHeader.substring(7);

                if (blackListToken.isTokenBlackList(token)) {
                    return onError(exchange, "Token is blacklisted", HttpStatus.UNAUTHORIZED, path);
                }

                if (!jwtTokenProvider.validateToken(token)) {
                    return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED, path);
                }

                String username = jwtTokenProvider.getUsernameFromToken(token);
                String userId = jwtTokenProvider.getUserIdFromToken(token);
                String authProvider = jwtTokenProvider.getClaimFromToken(token, "auth_provider");

                if (username == null || userId == null) {
                    return onError(exchange, "Token does not contain user information", HttpStatus.UNAUTHORIZED, path);
                }

                exchange.getRequest().mutate()
                        .header("X-UserId", userId)
                        .header("X-Username", username)
                        .header("X-Auth-Provider", authProvider)
                        .build();

                return chain.filter(exchange);
            } catch (RuntimeException e) {
                // Xử lý các exception liên quan đến token validation
                return onError(exchange, e.getMessage(), HttpStatus.UNAUTHORIZED, path);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus httpStatus, String path) {
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                message,
                path
        );
        BaseResponse<ErrorResponse> baseResponse = new BaseResponse<>(errorResponse);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(baseResponse);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
