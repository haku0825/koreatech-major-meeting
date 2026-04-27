package haku.kmm.org.koreatechmajormeeting.domain.post.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreatePostRequest(
    @Min(value = 1, message = "총 인원은 1명 이상이어야 합니다.")
    @Max(value = 10, message = "총 인원은 10명 이하여야 합니다.")
    int totalMemberCount,

    @NotBlank(message = "소개글은 필수입니다.")
    @Size(max = 500, message = "소개글은 500자 이하여야 합니다.")
    String introduction,

    @NotEmpty(message = "멤버 정보는 최소 1명 이상 필요합니다.")
    List<@Valid PostMemberRequest> memberProfiles
) {
}
