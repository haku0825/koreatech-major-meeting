package haku.kmm.org.koreatechmajormeeting.domain.post.repository;

import haku.kmm.org.koreatechmajormeeting.domain.post.entity.Post;
import haku.kmm.org.koreatechmajormeeting.domain.post.entity.PostStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
    boolean existsByWriterUserIdAndStatus(Long writerUserId, PostStatus status);

    boolean existsByWriterUserIdAndStatusIn(Long writerUserId, Collection<PostStatus> statuses);

    @EntityGraph(attributePaths = "memberProfiles")
    Page<Post> findAllByStatusOrderByCreatedAtDesc(PostStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "memberProfiles")
    Optional<Post> findById(Long id);

    @EntityGraph(attributePaths = "memberProfiles")
    Optional<Post> findByIdAndWriterUserId(Long id, Long writerUserId);

    Optional<Post> findTopByWriterUserIdAndStatusInOrderByCreatedAtDesc(Long writerUserId, Collection<PostStatus> statuses);

    void deleteAllByWriterUserId(Long writerUserId);
}
