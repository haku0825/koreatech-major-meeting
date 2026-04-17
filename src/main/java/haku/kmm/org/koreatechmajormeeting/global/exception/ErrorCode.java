package haku.kmm.org.koreatechmajormeeting.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401", "인증이 필요합니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 내부 오류가 발생했습니다."),

    INVALID_KOREATECH_EMAIL(HttpStatus.BAD_REQUEST, "AUTH_400_1", "한기대 이메일(@koreatech.ac.kr)만 사용할 수 있습니다."),
    EMAIL_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, "AUTH_400_2", "이메일 인증 요청 이력이 없습니다."),
    EMAIL_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_400_3", "인증 코드가 올바르지 않습니다."),
    EMAIL_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH_400_4", "인증 코드가 만료되었습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "AUTH_400_5", "이메일 인증이 완료되지 않았습니다."),
    INVALID_LOGIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_401_1", "이메일 또는 비밀번호가 올바르지 않습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_1", "사용자를 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_409_1", "이미 가입된 이메일입니다."),
    STUDENT_NUMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_409_2", "이미 사용 중인 학번입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
