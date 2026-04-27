package haku.kmm.org.koreatechmajormeeting.domain.user.service;

import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.AuthTokenResponse;
import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.DeleteMyAccountRequest;
import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.LoginRequest;
import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.SignupRequest;
import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.UpdateMyProfileRequest;
import haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto.UserProfileResponse;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.User;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.UserRole;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.WithdrawnUser;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.EmailVerificationRepository;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.UserRepository;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.WithdrawnUserRepository;
import haku.kmm.org.koreatechmajormeeting.domain.post.repository.PostRepository;
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
    private final PostRepository postRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final WithdrawnUserRepository withdrawnUserRepository;
    private final StudentCardVerificationService studentCardVerificationService;
    private final UserVerificationService userVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthTokenResponse signup(SignupRequest request) {
        userVerificationService.assertKoreatechEmail(request.email());

        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.SIGNUP_PASSWORD_MISMATCH);
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        if (userRepository.existsByStudentNumber(request.studentNumber())) {
            throw new BusinessException(ErrorCode.STUDENT_NUMBER_ALREADY_EXISTS);
        }

        if (userRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        User user = User.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .name(request.name())
            .nickname(request.nickname())
            .birthYear(request.birthYear())
            .studentNumber(request.studentNumber())
            .major(request.major())
            .role(UserRole.USER)
            .emailVerified(false)
            .studentCardVerified(false)
            .build();

        User savedUser = userRepository.save(user);

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

        return toUserProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UpdateMyProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String requestedNickname = request.nickname().trim();
        String currentNickname = user.getNickname();
        boolean nicknameChanged = currentNickname == null || !currentNickname.equals(requestedNickname);
        if (nicknameChanged && userRepository.existsByNickname(requestedNickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        user.updateProfile(request.name(), request.major());
        user.updateNickname(requestedNickname);
        return toUserProfileResponse(user);
    }

    @Transactional
    public void deleteMyAccount(Long userId, DeleteMyAccountRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_ACCOUNT_DELETE_PASSWORD);
        }

        withdrawnUserRepository.save(
            WithdrawnUser.builder()
                .userId(user.getId())
                .name(user.getName())
                .studentNumber(user.getStudentNumber())
                .reason(request.reason().trim())
                .build()
        );

        postRepository.deleteAllByWriterUserId(user.getId());
        emailVerificationRepository.deleteByEmail(user.getEmail());
        studentCardVerificationService.deleteByUserId(user.getId());
        userRepository.delete(user);
    }

    @Transactional
    public void promoteToAdmin(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.updateRole(UserRole.ADMIN);
    }

    private UserProfileResponse toUserProfileResponse(User user) {
        return new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getNickname(),
            user.getBirthYear(),
            user.getStudentNumber(),
            user.getMajor(),
            user.getRole(),
            user.isEmailVerified(),
            user.isStudentCardVerified()
        );
    }
}
