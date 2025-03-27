package blog.collection.gateway_service.security;

import blog.collection.gateway_service.common.CommonString;
import blog.collection.gateway_service.exception.JwtValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.*;

@Component
public class JwtTokenProvider {
    @Value("${ra.jwt.secret}")
    private String SECRET_KEY;
    @Value("${ra.jwt.expiration}")
    private Long JWT_EXPIRATION;
    @Value("${ra.jwt.refresh-secret}")
    private String REFRESH_SECRET_KEY;
    @Value("${ra.jwt.refresh-expiration}")
    private Long REFRESH_JWT_EXPIRATION;

    // Lấy claim từ token
    public String getClaimFromToken(String token, String claimName) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get(claimName, String.class);
    }

    // Lấy username từ token
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public String getUserIdFromToken(String token) {
        return getClaimFromToken(token, "userId");
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY.getBytes())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new JwtValidationException(CommonString.EXPIRED_TOKEN);
        } catch (UnsupportedJwtException e) {
            throw new JwtValidationException(CommonString.UNSUPPORTED_TOKEN);
        } catch (MalformedJwtException e) {
            throw new JwtValidationException(CommonString.TOKEN_FORMAT_NOT_CORRECT);
        } catch (SignatureException e) {
            throw new JwtValidationException(CommonString.INVALID_TOKEN);
        } catch (IllegalArgumentException e) {
            throw new JwtValidationException(CommonString.TOKEN_IS_EMPTY);
        }
    }

    // Validate refresh token
    public boolean validateRefreshToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(REFRESH_SECRET_KEY.getBytes())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("Token has expired");
        } catch (UnsupportedJwtException e) {
            throw new RuntimeException("Unsupported token");
        } catch (MalformedJwtException e) {
            throw new RuntimeException("Token format is not correct");
        } catch (SignatureException e) {
            throw new RuntimeException("Invalid token");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Token is empty");
        }
    }

    public long getTimeRemainingOfToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration().getTime() - System.currentTimeMillis();
    }
}
