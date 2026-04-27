package haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto;

import java.time.LocalDateTime;

public record StudentCardPendingResponse(
    Long requestId,
    Long userId,
    String userEmail,
    String userName,
    String studentNumber,
    String major,
    String status,
    String rejectReason,
    LocalDateTime submittedAt,
    LocalDateTime reviewedAt
) {
}
