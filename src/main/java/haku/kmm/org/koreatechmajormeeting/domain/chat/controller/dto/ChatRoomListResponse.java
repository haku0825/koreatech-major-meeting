package haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto;

import java.util.List;

public record ChatRoomListResponse(
    List<ChatRoomSummaryResponse> rooms,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {
}
