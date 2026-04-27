package haku.kmm.org.koreatechmajormeeting.domain.user.controller;

import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.RejectStudentCardRequest;
import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.StudentCardPendingResponse;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.StudentCardVerificationStatus;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.UserRole;
import haku.kmm.org.koreatechmajormeeting.domain.user.service.StudentCardVerificationService;
import haku.kmm.org.koreatechmajormeeting.global.common.ApiResponse;
import haku.kmm.org.koreatechmajormeeting.global.exception.BusinessException;
import haku.kmm.org.koreatechmajormeeting.global.exception.ErrorCode;
import haku.kmm.org.koreatechmajormeeting.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/student-cards")
public class AdminStudentCardController {

    private final StudentCardVerificationService studentCardVerificationService;

    @GetMapping
    public ApiResponse<List<StudentCardPendingResponse>> list(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @RequestParam(required = false) StudentCardVerificationStatus status
    ) {
        assertAdmin(authenticatedUser);
        if (status == null) {
            return ApiResponse.ok(studentCardVerificationService.listAll());
        }
        return ApiResponse.ok(studentCardVerificationService.listByStatus(status));
    }

    @GetMapping("/pending")
    public ApiResponse<List<StudentCardPendingResponse>> listPending(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        assertAdmin(authenticatedUser);
        return ApiResponse.ok(studentCardVerificationService.listPending());
    }

    @GetMapping("/{requestId}/image")
    public ResponseEntity<Resource> getImage(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long requestId
    ) {
        assertAdmin(authenticatedUser);
        StudentCardVerificationService.StudentCardImage image = studentCardVerificationService.loadImage(requestId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, image.contentType())
            .body(image.resource());
    }

    @PostMapping("/{requestId}/approve")
    public ApiResponse<Void> approve(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long requestId
    ) {
        assertAdmin(authenticatedUser);
        studentCardVerificationService.approve(authenticatedUser.userId(), requestId);
        return ApiResponse.ok();
    }

    @PostMapping("/{requestId}/reject")
    public ApiResponse<Void> reject(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long requestId,
        @Valid @RequestBody RejectStudentCardRequest request
    ) {
        assertAdmin(authenticatedUser);
        studentCardVerificationService.reject(authenticatedUser.userId(), requestId, request.reason());
        return ApiResponse.ok();
    }

    private void assertAdmin(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (authenticatedUser.role() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
