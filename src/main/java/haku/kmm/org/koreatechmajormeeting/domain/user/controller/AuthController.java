package haku.kmm.org.koreatechmajormeeting.domain.user.controller;

import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.AuthTokenResponse;
import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.EmailSendRequest;
import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.EmailVerifyRequest;
import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.LoginRequest;
import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.SignupRequest;
import haku.kmm.org.koreatechmajormeeting.domain.user.service.AuthService;
import haku.kmm.org.koreatechmajormeeting.domain.user.service.UserVerificationService;
import haku.kmm.org.koreatechmajormeeting.global.common.ApiResponse;
import haku.kmm.org.koreatechmajormeeting.global.security.jwt.JwtCookieService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserVerificationService userVerificationService;
    private final AuthService authService;
    private final JwtCookieService jwtCookieService;

    @PostMapping("/email/send")
    public ApiResponse<Void> sendEmailCode(@Valid @RequestBody EmailSendRequest request) {
        userVerificationService.sendVerificationCode(request.email());
        return ApiResponse.ok();
    }

    @PostMapping("/email/verify")
    public ApiResponse<Void> verifyEmailCode(@Valid @RequestBody EmailVerifyRequest request) {
        userVerificationService.verifyCode(request.email(), request.code());
        return ApiResponse.ok();
    }

    @PostMapping("/signup")
    public ApiResponse<AuthTokenResponse> signup(
        @Valid @RequestBody SignupRequest request,
        HttpServletResponse response
    ) {
        AuthTokenResponse tokenResponse = authService.signup(request);
        jwtCookieService.addAccessTokenCookie(response, tokenResponse.accessToken());
        return ApiResponse.ok(tokenResponse);
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletResponse response
    ) {
        AuthTokenResponse tokenResponse = authService.login(request);
        jwtCookieService.addAccessTokenCookie(response, tokenResponse.accessToken());
        return ApiResponse.ok(tokenResponse);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        jwtCookieService.clearAccessTokenCookie(response);
        return ApiResponse.ok();
    }
}
