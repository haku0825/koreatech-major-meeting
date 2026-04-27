package haku.kmm.org.koreatechmajormeeting.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "student_card_verifications",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_card_user_id", columnNames = "userId")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentCardVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 255)
    private String storedFileName;

    @Column(nullable = false, length = 500)
    private String storedPath;

    @Column(nullable = false, length = 120)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudentCardVerificationStatus status;

    @Column(length = 500)
    private String rejectReason;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    private Long reviewedByUserId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private StudentCardVerification(
        Long userId,
        String originalFileName,
        String storedFileName,
        String storedPath,
        String contentType,
        StudentCardVerificationStatus status,
        String rejectReason,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        Long reviewedByUserId
    ) {
        this.userId = userId;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.storedPath = storedPath;
        this.contentType = contentType;
        this.status = status == null ? StudentCardVerificationStatus.PENDING : status;
        this.rejectReason = rejectReason;
        this.submittedAt = submittedAt == null ? LocalDateTime.now() : submittedAt;
        this.reviewedAt = reviewedAt;
        this.reviewedByUserId = reviewedByUserId;
    }

    public void resubmit(
        String originalFileName,
        String storedFileName,
        String storedPath,
        String contentType
    ) {
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.storedPath = storedPath;
        this.contentType = contentType;
        this.status = StudentCardVerificationStatus.PENDING;
        this.rejectReason = null;
        this.submittedAt = LocalDateTime.now();
        this.reviewedAt = null;
        this.reviewedByUserId = null;
    }

    public void approve(Long adminUserId) {
        this.status = StudentCardVerificationStatus.APPROVED;
        this.rejectReason = null;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedByUserId = adminUserId;
    }

    public void reject(Long adminUserId, String rejectReason) {
        this.status = StudentCardVerificationStatus.REJECTED;
        this.rejectReason = rejectReason;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedByUserId = adminUserId;
    }

    public boolean isPending() {
        return this.status == StudentCardVerificationStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
