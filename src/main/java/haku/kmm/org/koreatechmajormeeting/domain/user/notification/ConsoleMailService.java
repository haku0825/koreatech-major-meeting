package haku.kmm.org.koreatechmajormeeting.domain.user.notification;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConsoleMailService implements MailService {

    @Override
    public void sendVerificationCode(String email, String code, LocalDateTime expiresAt) {
        log.info("[Email Verification] to={}, code={}, expiresAt={}", email, code, expiresAt);
    }
}
