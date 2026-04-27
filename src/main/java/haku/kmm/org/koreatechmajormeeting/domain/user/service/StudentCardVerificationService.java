package haku.kmm.org.koreatechmajormeeting.domain.user.service;

import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.StudentCardPendingResponse;
import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.StudentCardStatusResponse;
import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.StudentCardUploadResponse;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.StudentCardVerification;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.StudentCardVerificationStatus;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.User;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.StudentCardVerificationRepository;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.UserRepository;
import haku.kmm.org.koreatechmajormeeting.global.exception.BusinessException;
import haku.kmm.org.koreatechmajormeeting.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StudentCardVerificationService {

    private static final String DEFAULT_IMAGE_EXTENSION = ".jpg";

    private final StudentCardVerificationRepository studentCardVerificationRepository;
    private final UserRepository userRepository;

    @Value("${app.storage.student-card-dir:uploads/student-cards}")
    private String storageDir;

    private Path storageRoot;

    @PostConstruct
    void initStorageDirectory() {
        try {
            storageRoot = Paths.get(storageDir).toAbsolutePath().normalize();
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException("학생증 이미지 저장 디렉터리 초기화 실패", e);
        }
    }

    @Transactional
    public StudentCardUploadResponse submit(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isStudentCardVerified()) {
            throw new BusinessException(ErrorCode.STUDENT_CARD_ALREADY_VERIFIED);
        }

        if (!user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED_FOR_STUDENT_CARD);
        }

        validateImageFile(file);

        String originalFileName = file.getOriginalFilename() == null ? "student-card" : file.getOriginalFilename();
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        String extension = extractExtension(originalFileName, contentType);
        String storedFileName = UUID.randomUUID() + extension;
        Path target = storageRoot.resolve(storedFileName).normalize();

        try {
            if (!target.startsWith(storageRoot)) {
                throw new BusinessException(ErrorCode.INVALID_STUDENT_CARD_FILE);
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        StudentCardVerification verification = studentCardVerificationRepository.findByUserId(userId)
            .map(existing -> {
                deleteFileQuietly(existing.getStoredPath());
                existing.resubmit(
                    originalFileName,
                    storedFileName,
                    target.toString(),
                    contentType
                );
                return existing;
            })
            .orElseGet(() -> StudentCardVerification.builder()
                .userId(userId)
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .storedPath(target.toString())
                .contentType(contentType)
                .status(StudentCardVerificationStatus.PENDING)
                .build()
            );

        user.markStudentCardUnverified();
        StudentCardVerification saved = studentCardVerificationRepository.save(verification);
        return new StudentCardUploadResponse(saved.getId(), saved.getStatus(), saved.getSubmittedAt());
    }

    @Transactional(readOnly = true)
    public StudentCardStatusResponse findMyStatus(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        StudentCardVerification latest = studentCardVerificationRepository.findByUserId(userId).orElse(null);
        if (latest == null) {
            return new StudentCardStatusResponse(
                user.isEmailVerified(),
                user.isStudentCardVerified(),
                null,
                null,
                null,
                null
            );
        }

        return new StudentCardStatusResponse(
            user.isEmailVerified(),
            user.isStudentCardVerified(),
            latest.getStatus(),
            latest.getRejectReason(),
            latest.getSubmittedAt(),
            latest.getReviewedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<StudentCardPendingResponse> listPending() {
        return listByStatus(StudentCardVerificationStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<StudentCardPendingResponse> listAll() {
        return studentCardVerificationRepository.findAllByOrderBySubmittedAtDesc()
            .stream()
            .map(this::toAdminResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentCardPendingResponse> listByStatus(StudentCardVerificationStatus status) {
        return studentCardVerificationRepository.findAllByStatusOrderBySubmittedAtDesc(status)
            .stream()
            .map(this::toAdminResponse)
            .toList();
    }

    @Transactional
    public void approve(Long adminUserId, Long requestId) {
        StudentCardVerification request = studentCardVerificationRepository.findById(requestId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_CARD_REQUEST_NOT_FOUND));
        if (!request.isPending()) {
            throw new BusinessException(ErrorCode.STUDENT_CARD_REQUEST_NOT_PENDING);
        }

        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        request.approve(adminUserId);
        user.markStudentCardVerified();
    }

    @Transactional
    public void reject(Long adminUserId, Long requestId, String reason) {
        StudentCardVerification request = studentCardVerificationRepository.findById(requestId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_CARD_REQUEST_NOT_FOUND));
        if (!request.isPending()) {
            throw new BusinessException(ErrorCode.STUDENT_CARD_REQUEST_NOT_PENDING);
        }

        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        request.reject(adminUserId, reason);
        user.markStudentCardUnverified();
    }

    @Transactional(readOnly = true)
    public StudentCardImage loadImage(Long requestId) {
        StudentCardVerification request = studentCardVerificationRepository.findById(requestId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_CARD_REQUEST_NOT_FOUND));

        try {
            Path path = Paths.get(request.getStoredPath()).toAbsolutePath().normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(ErrorCode.STUDENT_CARD_REQUEST_NOT_FOUND);
            }
            return new StudentCardImage(resource, request.getContentType());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        studentCardVerificationRepository.findByUserId(userId)
            .ifPresent(request -> deleteFileQuietly(request.getStoredPath()));
        studentCardVerificationRepository.deleteByUserId(userId);
    }

    private StudentCardPendingResponse toAdminResponse(StudentCardVerification request) {
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return new StudentCardPendingResponse(
            request.getId(),
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getStudentNumber(),
            user.getMajor().name(),
            request.getStatus().name(),
            request.getRejectReason(),
            request.getSubmittedAt(),
            request.getReviewedAt()
        );
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_STUDENT_CARD_FILE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.INVALID_STUDENT_CARD_FILE);
        }
    }

    private String extractExtension(String originalFileName, String contentType) {
        int index = originalFileName.lastIndexOf('.');
        if (index >= 0 && index < originalFileName.length() - 1) {
            String ext = originalFileName.substring(index).toLowerCase();
            if (ext.length() <= 10) {
                return ext;
            }
        }
        if ("image/png".equals(contentType)) {
            return ".png";
        }
        if ("image/webp".equals(contentType)) {
            return ".webp";
        }
        return DEFAULT_IMAGE_EXTENSION;
    }

    private void deleteFileQuietly(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(storedPath));
        } catch (IOException ignored) {
            // 파일 삭제 실패는 인증 흐름을 막지 않고 다음 요청에서 덮어쓴다.
        }
    }

    public record StudentCardImage(Resource resource, String contentType) {
    }
}
