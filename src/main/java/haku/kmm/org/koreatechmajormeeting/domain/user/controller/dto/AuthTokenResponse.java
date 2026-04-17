package haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto;

import java.time.LocalDateTime;

public record AuthTokenResponse(
    Long userId,
    String userName,
    String accessToken,
    String tokenType,
    LocalDateTime expiresAt
) {
    public static AuthTokenResponse of(
        Long userId,
        String userName,
        String accessToken,
        LocalDateTime expiresAt
    ) {
        return new AuthTokenResponse(userId, userName, accessToken, "Bearer", expiresAt);
    }
}
