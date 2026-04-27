package haku.kmm.org.koreatechmajormeeting.global.controller;

import haku.kmm.org.koreatechmajormeeting.domain.chat.entity.ChatMessage;
import haku.kmm.org.koreatechmajormeeting.domain.chat.entity.ChatRoom;
import haku.kmm.org.koreatechmajormeeting.domain.chat.repository.ChatMessageRepository;
import haku.kmm.org.koreatechmajormeeting.domain.chat.repository.ChatRoomRepository;
import haku.kmm.org.koreatechmajormeeting.domain.post.entity.Post;
import haku.kmm.org.koreatechmajormeeting.domain.post.repository.PostRepository;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.EmailVerification;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.StudentCardVerification;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.User;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.WithdrawnUser;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.EmailVerificationRepository;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.StudentCardVerificationRepository;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.UserRepository;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.WithdrawnUserRepository;
import haku.kmm.org.koreatechmajormeeting.domain.user.service.AuthService;
import haku.kmm.org.koreatechmajormeeting.global.common.ApiResponse;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dev/db")
public class DevDataController {

    private final UserRepository userRepository;
    private final WithdrawnUserRepository withdrawnUserRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final StudentCardVerificationRepository studentCardVerificationRepository;
    private final PostRepository postRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AuthService authService;

    @GetMapping("/summary")
    public ApiResponse<DbSummaryResponse> summary() {
        long users = userRepository.count();
        long withdrawnUsers = withdrawnUserRepository.count();
        long verifications = emailVerificationRepository.count();
        long studentCardVerifications = studentCardVerificationRepository.count();
        long posts = postRepository.count();
        long chatRooms = chatRoomRepository.count();
        long chatMessages = chatMessageRepository.count();
        return ApiResponse.ok(
            new DbSummaryResponse(
                users,
                withdrawnUsers,
                verifications,
                studentCardVerifications,
                posts,
                chatRooms,
                chatMessages,
                LocalDateTime.now()
            )
        );
    }

    @GetMapping("/users")
    public ApiResponse<List<UserRow>> users() {
        List<UserRow> rows = userRepository.findAll().stream()
            .sorted(Comparator.comparing(User::getId).reversed())
            .map(user -> new UserRow(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getBirthYear(),
                user.getStudentNumber(),
                user.getMajor().name(),
                user.getRole().name(),
                user.isEmailVerified(),
                user.isStudentCardVerified(),
                user.getCreatedAt(),
                user.getUpdatedAt()
            ))
            .toList();
        return ApiResponse.ok(rows);
    }

    @GetMapping("/withdrawn-users")
    public ApiResponse<List<WithdrawnUserRow>> withdrawnUsers() {
        List<WithdrawnUserRow> rows = withdrawnUserRepository.findAll().stream()
            .sorted(Comparator.comparing(WithdrawnUser::getId).reversed())
            .map(user -> new WithdrawnUserRow(
                user.getId(),
                user.getUserId(),
                user.getName(),
                user.getStudentNumber(),
                user.getReason(),
                user.getWithdrawnAt()
            ))
            .toList();
        return ApiResponse.ok(rows);
    }

    @GetMapping("/email-verifications")
    public ApiResponse<List<EmailVerificationRow>> emailVerifications() {
        List<EmailVerificationRow> rows = emailVerificationRepository.findAll().stream()
            .sorted(Comparator.comparing(EmailVerification::getId).reversed())
            .map(v -> new EmailVerificationRow(
                v.getId(),
                v.getEmail(),
                v.getCode(),
                v.isVerified(),
                v.getExpiresAt(),
                v.getCreatedAt(),
                v.getUpdatedAt()
            ))
            .toList();
        return ApiResponse.ok(rows);
    }

    @GetMapping("/posts")
    public ApiResponse<List<PostRow>> posts() {
        List<PostRow> rows = postRepository.findAll().stream()
            .sorted(Comparator.comparing(Post::getId).reversed())
            .map(post -> new PostRow(
                post.getId(),
                post.getWriterUserId(),
                post.getTotalMemberCount(),
                post.getIntroduction(),
                post.getStatus().name(),
                post.getMemberProfiles().stream()
                    .sorted(Comparator.comparingInt(member -> member.getMemberOrder()))
                    .map(member -> member.getMemberOrder() + ":" + member.getMajor().name() + "/" + member.getStudentNumber())
                    .toList(),
                post.getCreatedAt(),
                post.getUpdatedAt()
            ))
            .toList();
        return ApiResponse.ok(rows);
    }

    @GetMapping("/student-card-verifications")
    public ApiResponse<List<StudentCardVerificationRow>> studentCardVerifications() {
        List<StudentCardVerificationRow> rows = studentCardVerificationRepository.findAll().stream()
            .sorted(Comparator.comparing(StudentCardVerification::getId).reversed())
            .map(v -> new StudentCardVerificationRow(
                v.getId(),
                v.getUserId(),
                v.getStatus().name(),
                v.getOriginalFileName(),
                v.getStoredFileName(),
                v.getRejectReason(),
                v.getSubmittedAt(),
                v.getReviewedAt(),
                v.getReviewedByUserId(),
                v.getCreatedAt(),
                v.getUpdatedAt()
            ))
            .toList();
        return ApiResponse.ok(rows);
    }

    @GetMapping("/chat-rooms")
    public ApiResponse<List<ChatRoomRow>> chatRooms() {
        List<ChatRoomRow> rows = chatRoomRepository.findAll().stream()
            .sorted(Comparator.comparing(ChatRoom::getId).reversed())
            .map(room -> new ChatRoomRow(
                room.getId(),
                room.getPostId(),
                room.getRequesterUserId(),
                room.getPostWriterUserId(),
                room.getStatus().name(),
                room.getClosedAt(),
                room.getClosedByUserId(),
                room.getCloseReason(),
                room.getCreatedAt(),
                room.getUpdatedAt()
            ))
            .toList();
        return ApiResponse.ok(rows);
    }

    @GetMapping("/chat-messages")
    public ApiResponse<List<ChatMessageRow>> chatMessages() {
        List<ChatMessageRow> rows = chatMessageRepository.findAll().stream()
            .sorted(Comparator.comparing(ChatMessage::getId).reversed())
            .map(message -> new ChatMessageRow(
                message.getId(),
                message.getRoom().getId(),
                message.getSenderUserId(),
                message.getContent(),
                message.getCreatedAt()
            ))
            .toList();
        return ApiResponse.ok(rows);
    }

    @PostMapping("/admin/promote")
    public ApiResponse<Void> promoteAdmin(@RequestParam String email) {
        authService.promoteToAdmin(email);
        return ApiResponse.ok();
    }

    public record DbSummaryResponse(
        long userCount,
        long withdrawnUserCount,
        long emailVerificationCount,
        long studentCardVerificationCount,
        long postCount,
        long chatRoomCount,
        long chatMessageCount,
        LocalDateTime checkedAt
    ) {
    }

    public record UserRow(
        Long id,
        String email,
        String name,
        String nickname,
        String birthYear,
        String studentNumber,
        String major,
        String role,
        boolean emailVerified,
        boolean studentCardVerified,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record EmailVerificationRow(
        Long id,
        String email,
        String code,
        boolean verified,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record StudentCardVerificationRow(
        Long id,
        Long userId,
        String status,
        String originalFileName,
        String storedFileName,
        String rejectReason,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        Long reviewedByUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record PostRow(
        Long id,
        Long writerUserId,
        int totalMemberCount,
        String introduction,
        String status,
        List<String> memberProfiles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record ChatRoomRow(
        Long id,
        Long postId,
        Long requesterUserId,
        Long postWriterUserId,
        String status,
        LocalDateTime closedAt,
        Long closedByUserId,
        String closeReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record ChatMessageRow(
        Long id,
        Long roomId,
        Long senderUserId,
        String content,
        LocalDateTime createdAt
    ) {
    }

    public record WithdrawnUserRow(
        Long id,
        Long userId,
        String name,
        String studentNumber,
        String reason,
        LocalDateTime withdrawnAt
    ) {
    }
}
