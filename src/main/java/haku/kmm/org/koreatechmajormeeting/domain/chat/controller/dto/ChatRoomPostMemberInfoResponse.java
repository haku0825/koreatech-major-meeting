package haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.Major;

public record ChatRoomPostMemberInfoResponse(
    int memberOrder,
    Major major,
    String studentNumber,
    String birthYear
) {
}
