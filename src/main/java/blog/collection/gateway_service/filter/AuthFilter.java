package blog.collection.gateway_service.filter;

import blog.collection.gateway_service.dto.BaseResponse;
import blog.collection.gateway_service.dto.ErrorResponse;
import org.springframework.web.server.ServerWebExchange;
import blog.collection.gateway_service.repository.BlackListTokenRepository;
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

@Component("AuthFilter")

public class AuthFilter extends AbstractGatewayFilterFactory<AbstractGatewayFilterFactory.NameConfig> {
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private BlackListTokenRepository blackListTokenRepository;
    @Autowired
    private ObjectMapper objectMapper;

    public AuthFilter() {
        super(NameConfig.class);
    }

    @Override
    public GatewayFilter apply(NameConfig config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().toString();
            if (path.startsWith("/blog-collection/auth/login") ||
                    path.startsWith("/blog-collection/auth/logout") ||
                    path.startsWith("/blog-collection/auth/sign-up") ||
                    path.startsWith("/blog-collection/auth/verify-email") ||
                    path.startsWith("/blog-collection/auth/success") ||
                    path.startsWith("/blog-collection/auth/failure") ||
                    path.startsWith("/blog-collection/auth/reset/verify") ||
                    path.startsWith("/blog-collection/auth/reset-password") ||
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

                if (blackListTokenRepository.isTokenBlackList(token)) {
                    return onError(exchange, "You need to login again!", HttpStatus.UNAUTHORIZED, path);
                }

                if (!jwtTokenProvider.validateToken(token)) {
                    return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED, path);
                }

                String username = jwtTokenProvider.getUsernameFromToken(token);
                String userId = jwtTokenProvider.getUserIdFromToken(token);
                String userAuthMethodId = jwtTokenProvider.getUserAuthMethodIdFromToken(token);
                String authProvider = (String) jwtTokenProvider.getClaimFromToken(token, "auth_provider");

                if (username == null || userId == null) {
                    return onError(exchange, "Token does not contain user information", HttpStatus.UNAUTHORIZED, path);
                }

                exchange.getRequest().mutate()
                        .header("X-UserId", userId)
                        .header("X-UserAuthMethodId", userAuthMethodId)
                        .header("X-Username", username)
                        .header("X-Auth-Provider", authProvider)
                        .build();

                return chain.filter(exchange);
            } catch (RuntimeException e) {
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
