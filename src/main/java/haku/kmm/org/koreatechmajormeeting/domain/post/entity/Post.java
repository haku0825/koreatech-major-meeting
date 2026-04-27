package haku.kmm.org.koreatechmajormeeting.domain.post.entity;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.Major;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long writerUserId;

    @Column(nullable = false)
    private int totalMemberCount;

    @Column(nullable = false, length = 500)
    private String introduction;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostMemberProfile> memberProfiles = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Post(
        Long writerUserId,
        int totalMemberCount,
        String introduction,
        PostStatus status
    ) {
        this.writerUserId = writerUserId;
        this.totalMemberCount = totalMemberCount;
        this.introduction = introduction;
        this.status = status == null ? PostStatus.RECRUITING : status;
    }

    public void updateIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public void replaceMemberProfiles(List<MemberProfileInput> inputs) {
        this.memberProfiles.clear();
        int order = 1;
        for (MemberProfileInput input : inputs) {
            this.memberProfiles.add(
                PostMemberProfile.of(this, order++, input.major(), input.studentNumber())
            );
        }
        this.totalMemberCount = inputs.size();
    }

    public boolean isWriter(Long userId) {
        return this.writerUserId.equals(userId);
    }

    public boolean isRecruiting() {
        return this.status == PostStatus.RECRUITING;
    }

    public boolean isInChat() {
        return this.status == PostStatus.IN_CHAT;
    }

    public boolean isClosed() {
        return this.status == PostStatus.CLOSED;
    }

    public void markInChat() {
        this.status = PostStatus.IN_CHAT;
    }

    public void reopenRecruiting() {
        this.status = PostStatus.RECRUITING;
    }

    public void close() {
        this.status = PostStatus.CLOSED;
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

    public record MemberProfileInput(
        Major major,
        String studentNumber
    ) {
    }
}
