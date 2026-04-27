package haku.kmm.org.koreatechmajormeeting.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_403", "권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 내부 오류가 발생했습니다."),

    INVALID_KOREATECH_EMAIL(HttpStatus.BAD_REQUEST, "AUTH_400_1", "한기대 이메일(@koreatech.ac.kr)만 사용할 수 있습니다."),
    EMAIL_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, "AUTH_400_2", "이메일 인증 요청 이력이 없습니다."),
    EMAIL_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_400_3", "인증 코드가 올바르지 않습니다."),
    EMAIL_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH_400_4", "인증 코드가 만료되었습니다."),
    INVALID_LOGIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_401_1", "이메일 또는 비밀번호가 올바르지 않습니다."),
    EMAIL_NOT_VERIFIED_FOR_LOGIN(HttpStatus.UNAUTHORIZED, "AUTH_401_2", "이메일 인증 후 로그인할 수 있습니다."),
    STUDENT_CARD_NOT_VERIFIED_FOR_LOGIN(HttpStatus.UNAUTHORIZED, "AUTH_401_3", "학생증 인증 승인 후 로그인할 수 있습니다."),
    EMAIL_NOT_VERIFIED_FOR_STUDENT_CARD(HttpStatus.BAD_REQUEST, "AUTH_400_5", "이메일 인증 완료 후 학생증을 업로드할 수 있습니다."),
    INVALID_STUDENT_CARD_FILE(HttpStatus.BAD_REQUEST, "AUTH_400_6", "학생증 이미지는 비어있지 않은 image/* 파일이어야 합니다."),
    STUDENT_CARD_FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "AUTH_400_7", "학생증 이미지 용량은 최대 10MB까지 업로드할 수 있습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_1", "사용자를 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_409_1", "이미 가입된 이메일입니다."),
    STUDENT_NUMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_409_2", "이미 사용 중인 학번입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_409_3", "이미 사용 중인 닉네임입니다."),
    SIGNUP_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "USER_400_3", "비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    INVALID_ACCOUNT_DELETE_PASSWORD(HttpStatus.UNAUTHORIZED, "USER_401_1", "회원 탈퇴 비밀번호가 올바르지 않습니다."),
    STUDENT_CARD_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_2", "학생증 인증 요청을 찾을 수 없습니다."),
    STUDENT_CARD_REQUEST_NOT_PENDING(HttpStatus.BAD_REQUEST, "USER_400_1", "대기 중(PENDING) 학생증 인증 요청이 아닙니다."),
    STUDENT_CARD_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, "USER_400_2", "학생증 인증이 이미 완료되어 변경할 수 없습니다."),

    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_404_1", "포스트를 찾을 수 없습니다."),
    POST_FORBIDDEN(HttpStatus.FORBIDDEN, "POST_403_1", "해당 포스트에 대한 권한이 없습니다."),
    POST_ALREADY_EXISTS(HttpStatus.CONFLICT, "POST_409_1", "한 사용자는 동시에 하나의 포스트만 작성할 수 있습니다."),
    POST_MEMBER_COUNT_MISMATCH(HttpStatus.BAD_REQUEST, "POST_400_1", "입력한 인원수와 멤버 정보 수가 일치하지 않습니다."),
    POST_MEMBER_STUDENT_NOT_FOUND(HttpStatus.BAD_REQUEST, "POST_400_2", "포스트 멤버 학번이 가입된 사용자 DB에 존재하지 않습니다."),
    POST_MEMBER_MAJOR_MISMATCH(HttpStatus.BAD_REQUEST, "POST_400_3", "포스트 멤버의 학번-학과 정보가 사용자 정보와 일치하지 않습니다."),
    POST_MEMBER_STUDENT_DUPLICATED(HttpStatus.BAD_REQUEST, "POST_400_4", "포스트 멤버 학번은 중복될 수 없습니다."),
    POST_WRITER_NOT_INCLUDED(HttpStatus.BAD_REQUEST, "POST_400_5", "포스트 작성자 본인의 학번을 멤버에 포함해야 합니다."),
    POST_NOT_RECRUITING(HttpStatus.BAD_REQUEST, "POST_400_6", "모집 중(RECRUITING) 상태의 포스트만 처리할 수 있습니다."),

    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_404_1", "채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_FORBIDDEN(HttpStatus.FORBIDDEN, "CHAT_403_1", "해당 채팅방에 대한 권한이 없습니다."),
    CHAT_SELF_ROOM_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CHAT_400_1", "본인 포스트에는 채팅방을 생성할 수 없습니다."),
    CHAT_TEAM_MEMBER_ROOM_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CHAT_400_2", "본인이 포함된 팀 포스트에는 채팅방을 생성할 수 없습니다."),
    CHAT_ROOM_LEAVE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CHAT_400_3", "포스트 작성자는 채팅방 나가기를 사용할 수 없습니다. 종료 기능을 사용하세요."),
    CHAT_ROOM_CLOSED(HttpStatus.BAD_REQUEST, "CHAT_400_4", "종료된 채팅방에는 메시지를 보낼 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
