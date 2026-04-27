package haku.kmm.org.koreatechmajormeeting.domain.post.controller.dto;

import haku.kmm.org.koreatechmajormeeting.domain.post.entity.PostStatus;
import java.time.LocalDateTime;
import java.util.List;

public record CreatePostResponse(
    Long postId,
    Long writerUserId,
    int totalMemberCount,
    String introduction,
    PostStatus status,
    LocalDateTime createdAt,
    List<PostMemberResponse> memberProfiles
) {
}
