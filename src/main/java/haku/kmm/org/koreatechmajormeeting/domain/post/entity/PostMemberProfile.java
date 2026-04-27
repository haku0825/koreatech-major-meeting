package haku.kmm.org.koreatechmajormeeting.domain.post.entity;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.Major;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "post_member_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostMemberProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false)
    private int memberOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private Major major;

    @Column(nullable = false, length = 20)
    private String studentNumber;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private PostMemberProfile(Post post, int memberOrder, Major major, String studentNumber) {
        this.post = post;
        this.memberOrder = memberOrder;
        this.major = major;
        this.studentNumber = studentNumber;
    }

    public static PostMemberProfile of(Post post, int memberOrder, Major major, String studentNumber) {
        return new PostMemberProfile(post, memberOrder, major, studentNumber);
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
