package haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.Major;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.UserRole;

public record UserProfileResponse(
    Long userId,
    String email,
    String name,
    String nickname,
    String birthYear,
    String studentNumber,
    Major major,
    UserRole role,
    boolean emailVerified,
    boolean studentCardVerified
) {
}
