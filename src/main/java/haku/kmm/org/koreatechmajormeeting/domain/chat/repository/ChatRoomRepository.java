package haku.kmm.org.koreatechmajormeeting.domain.chat.repository;

import haku.kmm.org.koreatechmajormeeting.domain.chat.entity.ChatRoom;
import haku.kmm.org.koreatechmajormeeting.domain.chat.entity.ChatRoomStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByPostIdAndRequesterUserId(Long postId, Long requesterUserId);

    Page<ChatRoom> findAllByRequesterUserIdOrPostWriterUserIdOrderByUpdatedAtDesc(
        Long requesterUserId,
        Long postWriterUserId,
        Pageable pageable
    );

    List<ChatRoom> findAllByPostId(Long postId);

    boolean existsByPostIdAndRequesterUserIdAndStatus(Long postId, Long requesterUserId, ChatRoomStatus status);

    void deleteAllByPostId(Long postId);
}
