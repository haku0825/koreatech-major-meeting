package haku.kmm.org.koreatechmajormeeting.domain.post.controller.dto;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.Major;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostMemberRequest(
    @NotNull(message = "멤버 학과는 필수입니다.")
    Major major,

    @NotBlank(message = "멤버 학번은 필수입니다.")
    @Size(min = 8, max = 20, message = "멤버 학번 길이가 올바르지 않습니다.")
    String studentNumber
) {
}
