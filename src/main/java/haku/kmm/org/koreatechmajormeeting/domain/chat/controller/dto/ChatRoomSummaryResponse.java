package haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto;

import java.time.LocalDateTime;

public record ChatRoomSummaryResponse(
    Long roomId,
    Long postId,
    Long postWriterUserId,
    Long otherUserId,
    String otherUserName,
    String status,
    Long lastMessageId,
    String lastMessageContent,
    LocalDateTime lastMessageAt,
    LocalDateTime updatedAt,
    LocalDateTime closedAt,
    Long closedByUserId,
    String closeReason
) {
}
