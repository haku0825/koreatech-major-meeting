package haku.kmm.org.koreatechmajormeeting.domain.user.repository;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.EmailVerification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByEmail(String email);

    void deleteByEmail(String email);
}
