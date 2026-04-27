package haku.kmm.org.koreatechmajormeeting.domain.chat.controller;

import haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto.ChatMessageListResponse;
import haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto.ChatMessageResponse;
import haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto.ChatRoomContextResponse;
import haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto.ChatRoomListResponse;
import haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto.CreateChatRoomRequest;
import haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto.CreateChatRoomResponse;
import haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto.SendChatMessageRequest;
import haku.kmm.org.koreatechmajormeeting.domain.chat.service.ChatService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chats")
public class ChatController {

    private final ChatService chatService;
    private final UserVerificationService userVerificationService;

    @PostMapping("/rooms")
    public ApiResponse<CreateChatRoomResponse> createRoom(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @Valid @RequestBody CreateChatRoomRequest request
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        userVerificationService.assertFullyVerified(authenticatedUser.userId());
        return ApiResponse.ok(chatService.createRoom(authenticatedUser.userId(), request));
    }

    @GetMapping("/rooms")
    public ApiResponse<ChatRoomListResponse> myRooms(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        userVerificationService.assertFullyVerified(authenticatedUser.userId());
        return ApiResponse.ok(chatService.listMyRooms(authenticatedUser.userId(), page, size));
    }

    @GetMapping("/rooms/{roomId}/context")
    public ApiResponse<ChatRoomContextResponse> roomContext(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long roomId
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        userVerificationService.assertFullyVerified(authenticatedUser.userId());
        return ApiResponse.ok(chatService.getRoomContext(authenticatedUser.userId(), roomId));
    }

    @PostMapping("/rooms/{roomId}/messages")
    public ApiResponse<ChatMessageResponse> sendMessage(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long roomId,
        @Valid @RequestBody SendChatMessageRequest request
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        userVerificationService.assertFullyVerified(authenticatedUser.userId());
        return ApiResponse.ok(chatService.sendMessage(authenticatedUser.userId(), roomId, request));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ApiResponse<ChatMessageListResponse> messages(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long roomId,
        @RequestParam(required = false) Long beforeMessageId,
        @RequestParam(defaultValue = "30") int size
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        userVerificationService.assertFullyVerified(authenticatedUser.userId());
        return ApiResponse.ok(chatService.listMessages(authenticatedUser.userId(), roomId, beforeMessageId, size));
    }

    @DeleteMapping("/rooms/{roomId}")
    public ApiResponse<Void> deleteRoom(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long roomId
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        userVerificationService.assertFullyVerified(authenticatedUser.userId());
        chatService.deleteRoom(authenticatedUser.userId(), roomId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/rooms/{roomId}/leave")
    public ApiResponse<Void> leaveRoom(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long roomId
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        userVerificationService.assertFullyVerified(authenticatedUser.userId());
        chatService.leaveRoom(authenticatedUser.userId(), roomId);
        return ApiResponse.ok();
    }
}
