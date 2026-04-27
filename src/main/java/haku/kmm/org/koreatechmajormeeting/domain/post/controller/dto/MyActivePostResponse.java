package haku.kmm.org.koreatechmajormeeting.domain.post.controller.dto;

import haku.kmm.org.koreatechmajormeeting.domain.post.entity.PostStatus;

public record MyActivePostResponse(
    boolean hasActivePost,
    Long postId,
    PostStatus status,
    int totalMemberCount,
    String introduction
) {
}
