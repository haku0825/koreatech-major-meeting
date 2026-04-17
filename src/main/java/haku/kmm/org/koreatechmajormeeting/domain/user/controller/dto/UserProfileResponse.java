package haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.Major;

public record UserProfileResponse(
    Long userId,
    String email,
    String name,
    String studentNumber,
    Major major
) {
}
