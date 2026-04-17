package haku.kmm.org.koreatechmajormeeting.global.security.jwt;

import java.time.LocalDateTime;

public record JwtToken(
    String accessToken,
    LocalDateTime expiresAt
) {
}
