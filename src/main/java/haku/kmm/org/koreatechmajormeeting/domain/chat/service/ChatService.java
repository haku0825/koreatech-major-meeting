package haku.kmm.org.koreatechmajormeeting.domain.chat.service;

import haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto.ChatMessageListResponse;
import haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto.ChatMessageResponse;
import haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto.ChatRoomContextResponse;
import haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto.ChatRoomListResponse;
import haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto.ChatRoomPostMemberInfoResponse;
import haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto.ChatRoomSummaryResponse;
import haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto.CreateChatRoomRequest;
import haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto.CreateChatRoomResponse;
import haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto.SendChatMessageRequest;
import haku.kmm.org.koreatechmajormeeting.domain.chat.entity.ChatMessage;
import haku.kmm.org.koreatechmajormeeting.domain.chat.entity.ChatRoom;
import haku.kmm.org.koreatechmajormeeting.domain.chat.repository.ChatMessageRepository;
import haku.kmm.org.koreatechmajormeeting.domain.chat.repository.ChatRoomRepository;
import haku.kmm.org.koreatechmajormeeting.domain.post.entity.Post;
import haku.kmm.org.koreatechmajormeeting.domain.post.repository.PostRepository;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.User;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.UserRepository;
import haku.kmm.org.koreatechmajormeeting.global.exception.BusinessException;
import haku.kmm.org.koreatechmajormeeting.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final long DUPLICATE_MESSAGE_BLOCK_SECONDS = 2L;
    private static final String CLOSE_REASON_POST_CLOSED = "POST_CLOSED";
    private static final String CLOSE_REASON_REQUESTER_LEFT = "REQUESTER_LEFT";

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateChatRoomResponse createRoom(Long requesterUserId, CreateChatRoomRequest request) {
        User requester = userRepository.findById(requesterUserId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Post post = postRepository.findById(request.postId())
            .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (post.getWriterUserId().equals(requesterUserId)) {
            throw new BusinessException(ErrorCode.CHAT_SELF_ROOM_NOT_ALLOWED);
        }
        boolean requesterIsPostMember = post.getMemberProfiles().stream()
            .anyMatch(member -> member.getStudentNumber().equals(requester.getStudentNumber()));
        if (requesterIsPostMember) {
            throw new BusinessException(ErrorCode.CHAT_TEAM_MEMBER_ROOM_NOT_ALLOWED);
        }

        ChatRoom existing = chatRoomRepository.findByPostIdAndRequesterUserId(request.postId(), requesterUserId)
            .orElse(null);
        if (existing != null) {
            if (existing.isClosed()) {
                if (!post.isRecruiting() && !post.isInChat()) {
                    throw new BusinessException(ErrorCode.POST_NOT_RECRUITING);
                }
                existing.reopen();
            }
            if (!post.isInChat()) {
                if (!post.isRecruiting()) {
                    throw new BusinessException(ErrorCode.POST_NOT_RECRUITING);
                }
                post.markInChat();
            }
            return new CreateChatRoomResponse(
                existing.getId(),
                existing.getPostId(),
                existing.getRequesterUserId(),
                existing.getPostWriterUserId(),
                existing.getStatus().name(),
                false,
                existing.getCreatedAt()
            );
        }

        if (!post.isRecruiting()) {
            throw new BusinessException(ErrorCode.POST_NOT_RECRUITING);
        }

        ChatRoom room = ChatRoom.builder()
            .postId(request.postId())
            .requesterUserId(requesterUserId)
            .postWriterUserId(post.getWriterUserId())
            .build();

        ChatRoom saved;
        try {
            saved = chatRoomRepository.save(room);
        } catch (DataIntegrityViolationException e) {
            ChatRoom raced = chatRoomRepository.findByPostIdAndRequesterUserId(request.postId(), requesterUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
            if (raced.isClosed()) {
                raced.reopen();
            }
            if (!post.isInChat()) {
                post.markInChat();
            }
            return new CreateChatRoomResponse(
                raced.getId(),
                raced.getPostId(),
                raced.getRequesterUserId(),
                raced.getPostWriterUserId(),
                raced.getStatus().name(),
                false,
                raced.getCreatedAt()
            );
        }

        post.markInChat();

        return new CreateChatRoomResponse(
            saved.getId(),
            saved.getPostId(),
            saved.getRequesterUserId(),
            saved.getPostWriterUserId(),
            saved.getStatus().name(),
            true,
            saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public ChatRoomListResponse listMyRooms(Long userId, int page, int size) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Page<ChatRoom> roomPage = chatRoomRepository.findAllByRequesterUserIdOrPostWriterUserIdOrderByUpdatedAtDesc(
            userId,
            userId,
            PageRequest.of(safePage, safeSize)
        );

        Set<Long> otherUserIds = roomPage.getContent().stream()
            .map(room -> room.findOtherUserId(userId))
            .filter(id -> id != null)
            .collect(Collectors.toSet());

        Map<Long, User> usersById = userRepository.findAllById(otherUserIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

        List<ChatRoomSummaryResponse> rooms = roomPage.getContent().stream()
            .map(room -> {
                Long otherUserId = room.findOtherUserId(userId);
                User otherUser = usersById.get(otherUserId);
                ChatMessage lastMessage = chatMessageRepository.findTopByRoom_IdOrderByIdDesc(room.getId()).orElse(null);

                return new ChatRoomSummaryResponse(
                    room.getId(),
                    room.getPostId(),
                    room.getPostWriterUserId(),
                    otherUserId,
                    otherUser == null ? "알 수 없음" : toRoomDisplayName(otherUser),
                    room.getStatus().name(),
                    lastMessage == null ? null : lastMessage.getId(),
                    lastMessage == null ? null : lastMessage.getContent(),
                    lastMessage == null ? null : lastMessage.getCreatedAt(),
                    room.getUpdatedAt(),
                    room.getClosedAt(),
                    room.getClosedByUserId(),
                    room.getCloseReason()
                );
            })
            .toList();

        return new ChatRoomListResponse(
            rooms,
            roomPage.getNumber(),
            roomPage.getSize(),
            roomPage.getTotalElements(),
            roomPage.getTotalPages(),
            roomPage.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public ChatRoomContextResponse getRoomContext(Long userId, Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!room.isParticipant(userId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_FORBIDDEN);
        }

        Post post = postRepository.findById(room.getPostId())
            .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        Long otherUserId = room.findOtherUserId(userId);
        User otherUser = otherUserId == null
            ? null
            : userRepository.findById(otherUserId).orElse(null);

        Set<String> studentNumbers = post.getMemberProfiles().stream()
            .map(member -> member.getStudentNumber())
            .collect(Collectors.toSet());
        Map<String, String> birthYearByStudentNumber = studentNumbers.isEmpty()
            ? Map.of()
            : userRepository.findAllByStudentNumberIn(studentNumbers).stream()
                .collect(Collectors.toMap(User::getStudentNumber, User::getBirthYear, (left, right) -> left));

        List<ChatRoomPostMemberInfoResponse> postMembers = post.getMemberProfiles().stream()
            .map(member -> new ChatRoomPostMemberInfoResponse(
                member.getMemberOrder(),
                member.getMajor(),
                member.getStudentNumber(),
                birthYearByStudentNumber.getOrDefault(member.getStudentNumber(), "-")
            ))
            .toList();

        return new ChatRoomContextResponse(
            room.getId(),
            room.getPostId(),
            room.getPostWriterUserId(),
            room.getRequesterUserId(),
            room.getStatus().name(),
            otherUser == null ? "알 수 없음" : toRoomDisplayName(otherUser),
            post.getIntroduction(),
            postMembers
        );
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long userId, Long roomId, SendChatMessageRequest request) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!room.isParticipant(userId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_FORBIDDEN);
        }
        if (room.isClosed()) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_CLOSED);
        }

        User sender = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String normalizedContent = request.content().trim();
        if (normalizedContent.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        ChatMessage latestMessage = chatMessageRepository.findTopByRoom_IdOrderByIdDesc(room.getId()).orElse(null);
        LocalDateTime duplicateBoundary = LocalDateTime.now().minusSeconds(DUPLICATE_MESSAGE_BLOCK_SECONDS);
        if (
            latestMessage != null &&
            latestMessage.getSenderUserId().equals(userId) &&
            latestMessage.getContent().equals(normalizedContent) &&
            !latestMessage.getCreatedAt().isBefore(duplicateBoundary)
        ) {
            return new ChatMessageResponse(
                latestMessage.getId(),
                room.getId(),
                latestMessage.getSenderUserId(),
                toRoomDisplayName(sender),
                latestMessage.getContent(),
                latestMessage.getCreatedAt()
            );
        }

        ChatMessage message = ChatMessage.builder()
            .room(room)
            .senderUserId(userId)
            .content(normalizedContent)
            .build();

        ChatMessage saved = chatMessageRepository.save(message);
        room.touch();

        return new ChatMessageResponse(
            saved.getId(),
            room.getId(),
            saved.getSenderUserId(),
            toRoomDisplayName(sender),
            saved.getContent(),
            saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public ChatMessageListResponse listMessages(Long userId, Long roomId, Long beforeMessageId, int size) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!room.isParticipant(userId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_FORBIDDEN);
        }

        int safeSize = Math.min(Math.max(size, 1), 100);
        int fetchSize = safeSize + 1;
        Pageable pageable = PageRequest.of(0, fetchSize);

        List<ChatMessage> fetched = beforeMessageId == null
            ? new ArrayList<>(chatMessageRepository.findAllByRoom_IdOrderByIdDesc(roomId, pageable))
            : new ArrayList<>(chatMessageRepository.findAllByRoom_IdAndIdLessThanOrderByIdDesc(roomId, beforeMessageId, pageable));

        boolean hasNext = fetched.size() > safeSize;
        if (hasNext) {
            fetched.remove(fetched.size() - 1);
        }

        Long nextCursorId = hasNext && !fetched.isEmpty()
            ? fetched.get(fetched.size() - 1).getId()
            : null;

        Set<Long> senderIds = fetched.stream().map(ChatMessage::getSenderUserId).collect(Collectors.toSet());
        Map<Long, User> usersById = userRepository.findAllById(senderIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

        Collections.reverse(fetched);
        List<ChatMessageResponse> messages = fetched.stream()
            .map(message -> {
                User sender = usersById.get(message.getSenderUserId());
                return new ChatMessageResponse(
                    message.getId(),
                    roomId,
                    message.getSenderUserId(),
                    sender == null ? "알 수 없음" : toRoomDisplayName(sender),
                    message.getContent(),
                    message.getCreatedAt()
                );
            })
            .toList();

        return new ChatMessageListResponse(
            messages,
            beforeMessageId,
            safeSize,
            hasNext,
            nextCursorId
        );
    }

    @Transactional
    public void deleteRoom(Long userId, Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!room.isParticipant(userId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_FORBIDDEN);
        }
        if (!room.getPostWriterUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_FORBIDDEN);
        }

        Long postId = room.getPostId();
        closePostAndRelatedRooms(userId, postId, null);
    }

    @Transactional
    public void leaveRoom(Long userId, Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!room.isParticipant(userId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_FORBIDDEN);
        }
        if (room.getPostWriterUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_LEAVE_NOT_ALLOWED);
        }
        if (room.isClosed()) {
            return;
        }
        room.close(userId, CLOSE_REASON_REQUESTER_LEFT);

        Post post = postRepository.findById(room.getPostId())
            .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        if (post.isInChat()) {
            post.reopenRecruiting();
        }
    }

    private void closePostAndRelatedRooms(Long closedByUserId, Long postId, Long alreadyClosedRoomId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.getWriterUserId().equals(closedByUserId) && alreadyClosedRoomId == null) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_FORBIDDEN);
        }

        post.close();
        List<ChatRoom> relatedRooms = chatRoomRepository.findAllByPostId(postId);
        relatedRooms.forEach(relatedRoom -> {
            if (alreadyClosedRoomId != null && relatedRoom.getId().equals(alreadyClosedRoomId)) {
                return;
            }
            relatedRoom.close(closedByUserId, CLOSE_REASON_POST_CLOSED);
        });
    }

    private String toRoomDisplayName(User user) {
        String nickname = user.getNickname();
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }
        String name = user.getName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        return "알 수 없음";
    }
}
