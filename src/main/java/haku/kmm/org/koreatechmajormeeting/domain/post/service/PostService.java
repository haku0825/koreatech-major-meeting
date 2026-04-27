package haku.kmm.org.koreatechmajormeeting.domain.post.service;

import haku.kmm.org.koreatechmajormeeting.domain.chat.repository.ChatRoomRepository;
import haku.kmm.org.koreatechmajormeeting.domain.chat.entity.ChatRoom;
import haku.kmm.org.koreatechmajormeeting.domain.chat.entity.ChatRoomStatus;
import haku.kmm.org.koreatechmajormeeting.domain.post.controller.dto.CreatePostRequest;
import haku.kmm.org.koreatechmajormeeting.domain.post.controller.dto.CreatePostResponse;
import haku.kmm.org.koreatechmajormeeting.domain.post.controller.dto.MyActivePostResponse;
import haku.kmm.org.koreatechmajormeeting.domain.post.controller.dto.PostListResponse;
import haku.kmm.org.koreatechmajormeeting.domain.post.controller.dto.PostMemberRequest;
import haku.kmm.org.koreatechmajormeeting.domain.post.controller.dto.PostMemberResponse;
import haku.kmm.org.koreatechmajormeeting.domain.post.controller.dto.PostSummaryResponse;
import haku.kmm.org.koreatechmajormeeting.domain.post.controller.dto.UpdatePostRequest;
import haku.kmm.org.koreatechmajormeeting.domain.post.entity.Post;
import haku.kmm.org.koreatechmajormeeting.domain.post.entity.PostStatus;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.User;
import haku.kmm.org.koreatechmajormeeting.domain.post.repository.PostRepository;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.UserRepository;
import haku.kmm.org.koreatechmajormeeting.global.exception.BusinessException;
import haku.kmm.org.koreatechmajormeeting.global.exception.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private static final String CLOSE_REASON_POST_CLOSED = "POST_CLOSED";

    @Transactional
    public CreatePostResponse create(Long writerUserId, CreatePostRequest request) {
        User writer = userRepository.findById(writerUserId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (postRepository.existsByWriterUserIdAndStatusIn(
            writerUserId,
            Set.of(PostStatus.RECRUITING, PostStatus.IN_CHAT)
        )) {
            throw new BusinessException(ErrorCode.POST_ALREADY_EXISTS);
        }

        validateMemberCount(request.totalMemberCount(), request.memberProfiles().size());
        validateMemberProfiles(request.memberProfiles(), writer.getStudentNumber());

        Post post = Post.builder()
            .writerUserId(writerUserId)
            .totalMemberCount(request.totalMemberCount())
            .introduction(request.introduction())
            .status(PostStatus.RECRUITING)
            .build();
        post.replaceMemberProfiles(toMemberInputs(request.memberProfiles()));

        Post saved = postRepository.save(post);
        return toCreateResponse(saved);
    }

    @Transactional
    public CreatePostResponse update(Long writerUserId, Long postId, UpdatePostRequest request) {
        User writer = userRepository.findById(writerUserId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        validateMemberCount(request.totalMemberCount(), request.memberProfiles().size());
        validateMemberProfiles(request.memberProfiles(), writer.getStudentNumber());

        Post post = postRepository.findByIdAndWriterUserId(postId, writerUserId)
            .orElseThrow(() -> {
                if (postRepository.existsById(postId)) {
                    return new BusinessException(ErrorCode.POST_FORBIDDEN);
                }
                return new BusinessException(ErrorCode.POST_NOT_FOUND);
            });
        if (!post.isRecruiting()) {
            throw new BusinessException(ErrorCode.POST_NOT_RECRUITING);
        }

        post.updateIntroduction(request.introduction());
        post.replaceMemberProfiles(toMemberInputs(request.memberProfiles()));
        return toCreateResponse(post);
    }

    @Transactional
    public void delete(Long writerUserId, Long postId) {
        Post post = postRepository.findByIdAndWriterUserId(postId, writerUserId)
            .orElseThrow(() -> {
                if (postRepository.existsById(postId)) {
                    return new BusinessException(ErrorCode.POST_FORBIDDEN);
                }
                return new BusinessException(ErrorCode.POST_NOT_FOUND);
            });
        if (post.isClosed()) {
            return;
        }

        post.close();
        List<ChatRoom> rooms = chatRoomRepository.findAllByPostId(post.getId());
        rooms.forEach(room -> room.close(writerUserId, CLOSE_REASON_POST_CLOSED));
    }

    @Transactional(readOnly = true)
    public PostListResponse listRecruitingPosts(Long viewerUserId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        User viewer = userRepository.findById(viewerUserId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Page<Post> posts = postRepository.findAllByStatusOrderByCreatedAtDesc(
            PostStatus.RECRUITING,
            PageRequest.of(safePage, safeSize)
        );

        Map<String, String> birthYearByStudentNumber = findBirthYearByStudentNumber(posts.getContent());
        List<PostSummaryResponse> items = posts.getContent().stream()
            .map(post -> toSummaryResponse(post, viewer, birthYearByStudentNumber))
            .toList();

        return new PostListResponse(
            items,
            posts.getNumber(),
            posts.getSize(),
            posts.getTotalElements(),
            posts.getTotalPages(),
            posts.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public MyActivePostResponse findMyActivePost(Long writerUserId) {
        if (!userRepository.existsById(writerUserId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return postRepository.findTopByWriterUserIdAndStatusInOrderByCreatedAtDesc(
                writerUserId,
                Set.of(PostStatus.RECRUITING, PostStatus.IN_CHAT)
            )
            .map(post -> new MyActivePostResponse(
                true,
                post.getId(),
                post.getStatus(),
                post.getTotalMemberCount(),
                post.getIntroduction()
            ))
            .orElseGet(() -> new MyActivePostResponse(false, null, null, 0, null));
    }

    private CreatePostResponse toCreateResponse(Post post) {
        Map<String, String> birthYearByStudentNumber = findBirthYearByStudentNumber(List.of(post));
        return new CreatePostResponse(
            post.getId(),
            post.getWriterUserId(),
            post.getTotalMemberCount(),
            post.getIntroduction(),
            post.getStatus(),
            post.getCreatedAt(),
            toMemberResponses(post, birthYearByStudentNumber)
        );
    }

    private PostSummaryResponse toSummaryResponse(
        Post post,
        User viewer,
        Map<String, String> birthYearByStudentNumber
    ) {
        boolean mine = post.getWriterUserId().equals(viewer.getId());
        boolean viewerIsTeamMember = post.getMemberProfiles().stream()
            .anyMatch(member -> member.getStudentNumber().equals(viewer.getStudentNumber()));
        boolean viewerHasOpenChatRoom = !mine && chatRoomRepository.existsByPostIdAndRequesterUserIdAndStatus(
            post.getId(),
            viewer.getId(),
            ChatRoomStatus.OPEN
        );

        return new PostSummaryResponse(
            post.getId(),
            post.getWriterUserId(),
            post.getTotalMemberCount(),
            post.getIntroduction(),
            post.getStatus(),
            post.getCreatedAt(),
            toMemberResponses(post, birthYearByStudentNumber),
            viewerIsTeamMember,
            viewerHasOpenChatRoom
        );
    }

    private List<PostMemberResponse> toMemberResponses(Post post, Map<String, String> birthYearByStudentNumber) {
        return post.getMemberProfiles().stream()
            .map(member -> new PostMemberResponse(
                member.getMemberOrder(),
                member.getMajor(),
                member.getStudentNumber(),
                birthYearByStudentNumber.getOrDefault(member.getStudentNumber(), "-")
            ))
            .toList();
    }

    private Map<String, String> findBirthYearByStudentNumber(List<Post> posts) {
        Set<String> studentNumbers = posts.stream()
            .flatMap(post -> post.getMemberProfiles().stream())
            .map(member -> member.getStudentNumber())
            .collect(Collectors.toSet());

        if (studentNumbers.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllByStudentNumberIn(studentNumbers).stream()
            .collect(Collectors.toMap(User::getStudentNumber, User::getBirthYear));
    }

    private List<Post.MemberProfileInput> toMemberInputs(List<PostMemberRequest> memberProfiles) {
        return memberProfiles.stream()
            .map(member -> new Post.MemberProfileInput(member.major(), member.studentNumber()))
            .toList();
    }

    private void validateMemberCount(int totalMemberCount, int actualMemberCount) {
        if (totalMemberCount != actualMemberCount) {
            throw new BusinessException(ErrorCode.POST_MEMBER_COUNT_MISMATCH);
        }
    }

    private void validateMemberProfiles(List<PostMemberRequest> memberProfiles, String writerStudentNumber) {
        Set<String> studentNumbers = new HashSet<>();
        boolean writerIncluded = false;

        for (PostMemberRequest member : memberProfiles) {
            if (!studentNumbers.add(member.studentNumber())) {
                throw new BusinessException(ErrorCode.POST_MEMBER_STUDENT_DUPLICATED);
            }

            User user = userRepository.findByStudentNumber(member.studentNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_MEMBER_STUDENT_NOT_FOUND));

            if (user.getMajor() != member.major()) {
                throw new BusinessException(ErrorCode.POST_MEMBER_MAJOR_MISMATCH);
            }

            if (member.studentNumber().equals(writerStudentNumber)) {
                writerIncluded = true;
            }
        }

        if (!writerIncluded) {
            throw new BusinessException(ErrorCode.POST_WRITER_NOT_INCLUDED);
        }
    }
}
