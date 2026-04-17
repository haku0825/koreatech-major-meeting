package haku.kmm.org.koreatechmajormeeting.global.security.jwt;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.User;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.UserRole;
import haku.kmm.org.koreatechmajormeeting.global.security.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValiditySeconds;

    public JwtTokenProvider(
        @Value("${security.jwt.secret}") String secret,
        @Value("${security.jwt.access-token-validity-seconds}") long accessTokenValiditySeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    public JwtToken issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenValiditySeconds);

        String token = Jwts.builder()
            .subject(user.getEmail())
            .claim("uid", user.getId())
            .claim("role", user.getRole().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(secretKey)
            .compact();

        LocalDateTime tokenExpiresAt = LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault());
        return new JwtToken(token, tokenExpiresAt);
    }

    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public AuthenticatedUser toAuthenticatedUser(String token) {
        Claims claims = parseClaims(token);
        Long userId = claims.get("uid", Long.class);
        String email = claims.getSubject();
        UserRole role = UserRole.valueOf(claims.get("role", String.class));
        return new AuthenticatedUser(userId, email, role);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
