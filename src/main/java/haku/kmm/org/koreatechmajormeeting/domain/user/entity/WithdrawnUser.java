package haku.kmm.org.koreatechmajormeeting.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "withdrawn_users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawnUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, length = 20)
    private String studentNumber;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime withdrawnAt;

    @Builder
    private WithdrawnUser(
        Long userId,
        String name,
        String studentNumber,
        String reason
    ) {
        this.userId = userId;
        this.name = name;
        this.studentNumber = studentNumber;
        this.reason = reason;
    }

    @PrePersist
    protected void onCreate() {
        this.withdrawnAt = LocalDateTime.now();
    }
}
