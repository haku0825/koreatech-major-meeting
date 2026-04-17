package haku.kmm.org.koreatechmajormeeting.domain.user.controller;

import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.UserProfileResponse;
import haku.kmm.org.koreatechmajormeeting.domain.user.service.AuthService;
import haku.kmm.org.koreatechmajormeeting.global.common.ApiResponse;
import haku.kmm.org.koreatechmajormeeting.global.exception.BusinessException;
import haku.kmm.org.koreatechmajormeeting.global.exception.ErrorCode;
import haku.kmm.org.koreatechmajormeeting.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return ApiResponse.ok(authService.findProfile(authenticatedUser.userId()));
    }
}
