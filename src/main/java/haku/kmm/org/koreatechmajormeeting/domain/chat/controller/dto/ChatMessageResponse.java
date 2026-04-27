package haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto;

import java.time.LocalDateTime;

public record ChatMessageResponse(
    Long messageId,
    Long roomId,
    Long senderUserId,
    String senderName,
    String content,
    LocalDateTime createdAt
) {
}
