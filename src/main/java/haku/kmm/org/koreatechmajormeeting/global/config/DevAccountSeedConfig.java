package haku.kmm.org.koreatechmajormeeting.global.config;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.EmailVerification;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.Major;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.StudentCardVerification;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.StudentCardVerificationStatus;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.User;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.UserRole;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.EmailVerificationRepository;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.StudentCardVerificationRepository;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DevAccountSeedConfig {

    private static final byte[] TINY_PNG = new byte[] {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
        0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
        (byte) 0x89, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x44, 0x41, 0x54,
        0x78, (byte) 0xDA, 0x63, (byte) 0xFC, (byte) 0xFF, (byte) 0x9F, (byte) 0xA1, 0x1E,
        0x00, 0x07, (byte) 0x82, 0x02, 0x7F, 0x3F, (byte) 0xBE, (byte) 0xB0,
        (byte) 0x94, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
        (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final StudentCardVerificationRepository studentCardVerificationRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.storage.student-card-dir:uploads/student-cards}")
    private String studentCardDir;

    @Bean
    @ConditionalOnProperty(
        name = "app.dev.seed-users.enabled",
        havingValue = "true"
    )
    public CommandLineRunner seedDevAccounts() {
        return args -> {
            List<SeedAccount> accounts = List.of(
                new SeedAccount("alpha@koreatech.ac.kr", "alpha1234!", "알파", "alpha", "2002", "2026000100", Major.COMPUTER_SCIENCE, UserRole.USER, true),
                new SeedAccount("bravo@koreatech.ac.kr", "bravo1234!", "브라보", "bravo", "2001", "2026000200", Major.MECHANICAL, UserRole.USER, true),
                new SeedAccount("charlie@koreatech.ac.kr", "charlie1234!", "찰리", "charlie", "2003", "2026000300", Major.ELECTRICAL_ELECTRONICS_COMMUNICATION, UserRole.USER, true),
                new SeedAccount("delta@koreatech.ac.kr", "delta1234!", "델타", "delta", "2000", "2026000400", Major.INDUSTRIAL_MANAGEMENT, UserRole.USER, false),
                new SeedAccount("admin@koreatech.ac.kr", "admin1234!", "관리자", "admin", "1998", "2026999900", Major.COMPUTER_SCIENCE, UserRole.ADMIN, true)
            );

            for (SeedAccount account : accounts) {
                User user = userRepository.findByEmail(account.email())
                    .map(existing -> {
                        existing.updatePassword(passwordEncoder.encode(account.password()));
                        existing.updateProfile(account.name(), account.major());
                        existing.updateNickname(account.nickname());
                        existing.updateBirthYear(account.birthYear());
                        existing.updateRole(account.role());
                        existing.markEmailVerified();
                        if (account.studentCardVerified()) {
                            existing.markStudentCardVerified();
                        } else {
                            existing.markStudentCardUnverified();
                        }
                        return existing;
                    })
                    .orElseGet(() -> User.builder()
                        .email(account.email())
                        .password(passwordEncoder.encode(account.password()))
                        .name(account.name())
                        .nickname(account.nickname())
                        .birthYear(account.birthYear())
                        .studentNumber(account.studentNumber())
                        .major(account.major())
                        .role(account.role())
                        .emailVerified(true)
                        .studentCardVerified(account.studentCardVerified())
                        .build()
                    );

                User saved = userRepository.save(user);
                upsertEmailVerification(saved.getEmail());
                if (account.studentCardVerified()) {
                    upsertApprovedStudentCard(saved.getId(), account.studentNumber());
                } else {
                    studentCardVerificationRepository.deleteByUserId(saved.getId());
                }
            }
        };
    }

    private void upsertEmailVerification(String email) {
        LocalDateTime expiresAt = LocalDateTime.now().plusYears(10);
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
            .map(existing -> {
                existing.refreshCode("999999", expiresAt);
                existing.markVerified();
                return existing;
            })
            .orElseGet(() -> EmailVerification.builder()
                .email(email)
                .code("999999")
                .expiresAt(expiresAt)
                .verified(true)
                .build()
            );
        emailVerificationRepository.save(verification);
    }

    private void upsertApprovedStudentCard(Long userId, String studentNumber) throws IOException {
        Path dir = Paths.get(studentCardDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        String fileName = "seed-" + studentNumber + ".png";
        Path imagePath = dir.resolve(fileName);
        if (!Files.exists(imagePath)) {
            Files.write(
                imagePath,
                TINY_PNG,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
        }

        StudentCardVerification verification = studentCardVerificationRepository.findByUserId(userId)
            .map(existing -> {
                existing.resubmit(
                    fileName,
                    fileName,
                    imagePath.toString(),
                    "image/png"
                );
                existing.approve(userId);
                return existing;
            })
            .orElseGet(() -> StudentCardVerification.builder()
                .userId(userId)
                .originalFileName(fileName)
                .storedFileName(fileName)
                .storedPath(imagePath.toString())
                .contentType("image/png")
                .status(StudentCardVerificationStatus.APPROVED)
                .submittedAt(LocalDateTime.now().minusDays(1))
                .reviewedAt(LocalDateTime.now().minusHours(12))
                .reviewedByUserId(userId)
                .build()
            );

        studentCardVerificationRepository.save(verification);
    }

    private record SeedAccount(
        String email,
        String password,
        String name,
        String nickname,
        String birthYear,
        String studentNumber,
        Major major,
        UserRole role,
        boolean studentCardVerified
    ) {
    }
}
