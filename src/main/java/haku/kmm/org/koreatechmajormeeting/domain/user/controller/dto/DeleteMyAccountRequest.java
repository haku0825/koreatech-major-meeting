package haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteMyAccountRequest(
    @NotBlank(message = "탈퇴 확인 비밀번호는 필수입니다.")
    @Size(min = 8, max = 50, message = "비밀번호는 8자 이상 50자 이하여야 합니다.")
    String password,

    @NotBlank(message = "탈퇴 사유는 필수입니다.")
    @Size(max = 500, message = "탈퇴 사유는 500자 이하여야 합니다.")
    String reason
) {
}
