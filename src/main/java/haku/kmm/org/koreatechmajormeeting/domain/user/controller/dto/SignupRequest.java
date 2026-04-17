package haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.Major;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    String email,

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 50, message = "비밀번호는 8자 이상 50자 이하여야 합니다.")
    String password,

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 30, message = "이름은 30자 이하여야 합니다.")
    String name,

    @NotBlank(message = "학번은 필수입니다.")
    @Size(min = 8, max = 20, message = "학번 길이가 올바르지 않습니다.")
    String studentNumber,

    @NotNull(message = "학과는 필수입니다.")
    Major major
) {
}
