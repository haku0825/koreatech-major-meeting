package haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto;

import java.time.LocalDateTime;

public record CreateChatRoomResponse(
    Long roomId,
    Long postId,
    Long requesterUserId,
    Long postWriterUserId,
    String status,
    boolean createdNew,
    LocalDateTime createdAt
) {
}
