package haku.kmm.org.koreatechmajormeeting.domain.post.controller.dto;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.Major;

public record PostMemberResponse(
    int memberOrder,
    Major major,
    String studentNumber,
    String birthYear
) {
}
