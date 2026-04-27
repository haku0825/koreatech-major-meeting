package haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.Major;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 30, message = "이름은 30자 이하여야 합니다.")
    String name,

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
    String nickname,

    @NotNull(message = "학과는 필수입니다.")
    Major major
) {
}
