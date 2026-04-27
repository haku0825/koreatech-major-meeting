package haku.kmm.org.koreatechmajormeeting.domain.user.repository;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.WithdrawnUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawnUserRepository extends JpaRepository<WithdrawnUser, Long> {
}
