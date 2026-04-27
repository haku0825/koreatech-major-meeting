package haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto;

import jakarta.validation.constraints.NotNull;

public record CreateChatRoomRequest(
    @NotNull(message = "포스트 ID는 필수입니다.")
    Long postId
) {
}
