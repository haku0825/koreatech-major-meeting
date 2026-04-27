package haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.StudentCardVerificationStatus;
import java.time.LocalDateTime;

public record StudentCardStatusResponse(
    boolean emailVerified,
    boolean studentCardVerified,
    StudentCardVerificationStatus latestRequestStatus,
    String rejectReason,
    LocalDateTime submittedAt,
    LocalDateTime reviewedAt
) {
}
