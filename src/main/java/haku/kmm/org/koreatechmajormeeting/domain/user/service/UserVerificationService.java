package haku.kmm.org.koreatechmajormeeting.domain.user.service;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.EmailVerification;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.User;
import haku.kmm.org.koreatechmajormeeting.domain.user.notification.MailService;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.EmailVerificationRepository;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.UserRepository;
import haku.kmm.org.koreatechmajormeeting.global.exception.BusinessException;
import haku.kmm.org.koreatechmajormeeting.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserVerificationService {

    private static final int CODE_LENGTH = 6;
    private static final long VERIFICATION_EXPIRATION_MINUTES = 10L;
    private static final Pattern KOREATECH_EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9._%+-]+@koreatech\\.ac\\.kr$");

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    @Transactional
    public void sendVerificationCode(String email) {
        assertKoreatechEmail(email);
        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(VERIFICATION_EXPIRATION_MINUTES);

        EmailVerification verification = emailVerificationRepository.findByEmail(email)
            .map(existing -> {
                existing.refreshCode(code, expiresAt);
                return existing;
            })
            .orElseGet(() -> EmailVerification.builder()
                .email(email)
                .code(code)
                .expiresAt(expiresAt)
                .verified(false)
                .build()
            );

        emailVerificationRepository.save(verification);
        mailService.sendVerificationCode(email, code, expiresAt);
    }

    @Transactional
    public void verifyCode(String email, String code) {
        assertKoreatechEmail(email);

        EmailVerification verification = emailVerificationRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_CODE_NOT_FOUND));

        if (verification.isExpired()) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_EXPIRED);
        }

        if (!verification.getCode().equals(code)) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_MISMATCH);
        }

        verification.markVerified();
        userRepository.findByEmail(email).ifPresent(User::markEmailVerified);
    }

    public void assertKoreatechEmail(String email) {
        if (!KOREATECH_EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(ErrorCode.INVALID_KOREATECH_EMAIL);
        }
    }

    @Transactional(readOnly = true)
    public void assertFullyVerified(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED_FOR_LOGIN);
        }

        if (!user.isStudentCardVerified()) {
            throw new BusinessException(ErrorCode.STUDENT_CARD_NOT_VERIFIED_FOR_LOGIN);
        }
    }

    private String generateCode() {
        int min = (int) Math.pow(10, CODE_LENGTH - 1);
        int max = (int) Math.pow(10, CODE_LENGTH) - 1;
        int value = ThreadLocalRandom.current().nextInt(min, max + 1);
        return String.valueOf(value);
    }
}
