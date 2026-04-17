package haku.kmm.org.koreatechmajormeeting.domain.user.service;

import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.AuthTokenResponse;
import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.LoginRequest;
import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.SignupRequest;
import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.UserProfileResponse;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.User;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.UserRole;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.UserRepository;
import haku.kmm.org.koreatechmajormeeting.global.exception.BusinessException;
import haku.kmm.org.koreatechmajormeeting.global.exception.ErrorCode;
import haku.kmm.org.koreatechmajormeeting.global.security.jwt.JwtToken;
import haku.kmm.org.koreatechmajormeeting.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserVerificationService userVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthTokenResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        if (userRepository.existsByStudentNumber(request.studentNumber())) {
            throw new BusinessException(ErrorCode.STUDENT_NUMBER_ALREADY_EXISTS);
        }

        userVerificationService.assertVerifiedForSignup(request.email());

        User user = User.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .name(request.name())
            .studentNumber(request.studentNumber())
            .major(request.major())
            .role(UserRole.USER)
            .build();

        User savedUser = userRepository.save(user);
        userVerificationService.clearVerification(request.email());

        JwtToken jwtToken = jwtTokenProvider.issue(savedUser);
        return AuthTokenResponse.of(
            savedUser.getId(),
            savedUser.getName(),
            jwtToken.accessToken(),
            jwtToken.expiresAt()
        );
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        JwtToken jwtToken = jwtTokenProvider.issue(user);
        return AuthTokenResponse.of(
            user.getId(),
            user.getName(),
            jwtToken.accessToken(),
            jwtToken.expiresAt()
        );
    }

    @Transactional(readOnly = true)
    public UserProfileResponse findProfile(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getStudentNumber(),
            user.getMajor()
        );
    }
}
