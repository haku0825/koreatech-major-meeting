package haku.kmm.org.koreatechmajormeeting.domain.post.controller.dto;

import java.util.List;

public record PostListResponse(
    List<PostSummaryResponse> posts,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {
}
