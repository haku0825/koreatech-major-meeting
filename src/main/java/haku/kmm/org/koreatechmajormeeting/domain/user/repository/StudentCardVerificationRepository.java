package haku.kmm.org.koreatechmajormeeting.domain.user.repository;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.StudentCardVerification;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.StudentCardVerificationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentCardVerificationRepository extends JpaRepository<StudentCardVerification, Long> {
    Optional<StudentCardVerification> findByUserId(Long userId);

    List<StudentCardVerification> findAllByStatusOrderBySubmittedAtAsc(StudentCardVerificationStatus status);

    List<StudentCardVerification> findAllByOrderBySubmittedAtDesc();

    List<StudentCardVerification> findAllByStatusOrderBySubmittedAtDesc(StudentCardVerificationStatus status);

    void deleteByUserId(Long userId);
}
