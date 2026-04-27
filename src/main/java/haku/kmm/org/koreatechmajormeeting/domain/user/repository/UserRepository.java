package haku.kmm.org.koreatechmajormeeting.domain.user.repository;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    boolean existsByStudentNumber(String studentNumber);

    boolean existsByNickname(String nickname);

    Optional<User> findByEmail(String email);

    Optional<User> findByStudentNumber(String studentNumber);

    List<User> findAllByStudentNumberIn(Collection<String> studentNumbers);
}
