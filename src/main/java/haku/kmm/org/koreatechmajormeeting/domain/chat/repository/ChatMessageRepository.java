package haku.kmm.org.koreatechmajormeeting.domain.chat.repository;

import haku.kmm.org.koreatechmajormeeting.domain.chat.entity.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Override
    @EntityGraph(attributePaths = "room")
    List<ChatMessage> findAll();

    Optional<ChatMessage> findTopByRoom_IdOrderByIdDesc(Long roomId);

    List<ChatMessage> findAllByRoom_IdOrderByIdDesc(Long roomId, Pageable pageable);

    List<ChatMessage> findAllByRoom_IdAndIdLessThanOrderByIdDesc(
        Long roomId,
        Long beforeMessageId,
        Pageable pageable
    );

    void deleteAllByRoom_Id(Long roomId);

    void deleteAllByRoom_PostId(Long postId);
}
