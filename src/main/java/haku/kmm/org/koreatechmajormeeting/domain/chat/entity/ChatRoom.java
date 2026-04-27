package haku.kmm.org.koreatechmajormeeting.domain.chat.entity;

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
    name = "chat_rooms",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_chat_room_post_requester", columnNames = {"postId", "requesterUserId"})
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private Long requesterUserId;

    @Column(nullable = false)
    private Long postWriterUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomStatus status;

    private LocalDateTime closedAt;

    private Long closedByUserId;

    @Column(length = 120)
    private String closeReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private ChatRoom(
        Long postId,
        Long requesterUserId,
        Long postWriterUserId,
        ChatRoomStatus status
    ) {
        this.postId = postId;
        this.requesterUserId = requesterUserId;
        this.postWriterUserId = postWriterUserId;
        this.status = status == null ? ChatRoomStatus.OPEN : status;
    }

    public boolean isParticipant(Long userId) {
        return this.requesterUserId.equals(userId) || this.postWriterUserId.equals(userId);
    }

    public Long findOtherUserId(Long userId) {
        if (this.requesterUserId.equals(userId)) {
            return this.postWriterUserId;
        }
        if (this.postWriterUserId.equals(userId)) {
            return this.requesterUserId;
        }
        return null;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isClosed() {
        return this.status == ChatRoomStatus.CLOSED;
    }

    public void close(Long closedByUserId, String closeReason) {
        if (isClosed()) {
            return;
        }
        this.status = ChatRoomStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
        this.closedByUserId = closedByUserId;
        this.closeReason = closeReason;
        touch();
    }

    public void reopen() {
        this.status = ChatRoomStatus.OPEN;
        this.closedAt = null;
        this.closedByUserId = null;
        this.closeReason = null;
        touch();
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = ChatRoomStatus.OPEN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
