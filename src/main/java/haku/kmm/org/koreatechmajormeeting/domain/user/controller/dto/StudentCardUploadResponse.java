package haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.StudentCardVerificationStatus;
import java.time.LocalDateTime;

public record StudentCardUploadResponse(
    Long requestId,
    StudentCardVerificationStatus status,
    LocalDateTime submittedAt
) {
}
