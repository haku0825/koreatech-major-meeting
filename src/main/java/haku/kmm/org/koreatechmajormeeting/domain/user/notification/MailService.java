package haku.kmm.org.koreatechmajormeeting.domain.user.notification;

import java.time.LocalDateTime;

public interface MailService {
    void sendVerificationCode(String email, String code, LocalDateTime expiresAt);
}
