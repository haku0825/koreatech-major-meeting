package haku.kmm.org.koreatechmajormeeting.domain.post.controller;

import haku.kmm.org.koreatechmajormeeting.domain.post.controller.dto.CreatePostRequest;
import haku.kmm.org.koreatechmajormeeting.domain.post.controller.dto.CreatePostResponse;
import haku.kmm.org.koreatechmajormeeting.domain.post.controller.dto.MyActivePostResponse;
import haku.kmm.org.koreatechmajormeeting.domain.post.controller.dto.PostListResponse;
import haku.kmm.org.koreatechmajormeeting.domain.post.controller.dto.UpdatePostRequest;
import haku.kmm.org.koreatechmajormeeting.domain.post.service.PostService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;
    private final UserVerificationService userVerificationService;

    @PostMapping
    public ApiResponse<CreatePostResponse> create(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @Valid @RequestBody CreatePostRequest request
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        userVerificationService.assertFullyVerified(authenticatedUser.userId());

        return ApiResponse.ok(postService.create(authenticatedUser.userId(), request));
    }

    @GetMapping
    public ApiResponse<PostListResponse> list(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        userVerificationService.assertFullyVerified(authenticatedUser.userId());

        return ApiResponse.ok(postService.listRecruitingPosts(authenticatedUser.userId(), page, size));
    }

    @GetMapping("/me/active")
    public ApiResponse<MyActivePostResponse> myActivePost(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        userVerificationService.assertFullyVerified(authenticatedUser.userId());
        return ApiResponse.ok(postService.findMyActivePost(authenticatedUser.userId()));
    }

    @PutMapping("/{postId}")
    public ApiResponse<CreatePostResponse> update(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long postId,
        @Valid @RequestBody UpdatePostRequest request
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        userVerificationService.assertFullyVerified(authenticatedUser.userId());
        return ApiResponse.ok(postService.update(authenticatedUser.userId(), postId, request));
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<Void> delete(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long postId
    ) {
        if (authenticatedUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        userVerificationService.assertFullyVerified(authenticatedUser.userId());
        postService.delete(authenticatedUser.userId(), postId);
        return ApiResponse.ok();
    }
}
