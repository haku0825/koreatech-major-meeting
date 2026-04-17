package haku.kmm.org.koreatechmajormeeting.global.security;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.UserRole;

public record AuthenticatedUser(
    Long userId,
    String email,
    UserRole role
) {
}
