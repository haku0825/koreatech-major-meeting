package haku.kmm.org.koreatechmajormeeting.domain.user.controller;

import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.StudentCardStatusResponse;
import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.StudentCardUploadResponse;
import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.DeleteMyAccountRequest;
import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.UpdateMyProfileRequest;
import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.UserProfileResponse;
import haku.kmm.org.koreatechmajormeeting.domain.user.service.AuthService;
import haku.kmm.org.koreatechmajormeeting.domain.user.service.StudentCardVerificationService;
import haku.kmm.org.koreatechmajormeeting.domain.user.service.UserVerificationService;
import haku.kmm.org.koreatechmajormeeting.global.common.ApiResponse;
import haku.kmm.org.koreatechmajormeeting.global.exception.BusinessException;
import haku.kmm.org.koreatechmajormeeting.global.exception.ErrorCode;
import haku.kmm.org.koreatechmajormeeting.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;
    private final StudentCardVerificationService studentCardVerificationService;
    private final UserVerificationService userVerificationService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return ApiResponse.ok(authService.findProfile(authenticatedUser.userId()));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateMe(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @Valid @RequestBody UpdateMyProfileRequest request
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        userVerificationService.assertFullyVerified(authenticatedUser.userId());
        return ApiResponse.ok(authService.updateMyProfile(authenticatedUser.userId(), request));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMe(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @Valid @RequestBody DeleteMyAccountRequest request
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        userVerificationService.assertFullyVerified(authenticatedUser.userId());
        authService.deleteMyAccount(authenticatedUser.userId(), request);
        return ApiResponse.ok();
    }

    @PostMapping("/me/student-card")
    public ApiResponse<StudentCardUploadResponse> uploadStudentCard(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @RequestParam("file") MultipartFile file
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(studentCardVerificationService.submit(authenticatedUser.userId(), file));
    }

    @GetMapping("/me/student-card/status")
    public ApiResponse<StudentCardStatusResponse> studentCardStatus(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(studentCardVerificationService.findMyStatus(authenticatedUser.userId()));
    }
}
