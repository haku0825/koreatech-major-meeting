package haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto;

import java.util.List;

public record ChatMessageListResponse(
    List<ChatMessageResponse> messages,
    Long beforeMessageId,
    int size,
    boolean hasNext,
    Long nextCursorId
) {
}
