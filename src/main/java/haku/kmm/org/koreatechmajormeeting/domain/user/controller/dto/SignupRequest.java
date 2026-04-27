package haku.kmm.org.koreatechmajormeeting.domain.user.controller.dto;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.Major;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    String email,

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 50, message = "비밀번호는 8자 이상 50자 이하여야 합니다.")
    String password,

    @NotBlank(message = "비밀번호 확인은 필수입니다.")
    String passwordConfirm,

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 30, message = "이름은 30자 이하여야 합니다.")
    String name,

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
    String nickname,

    @NotBlank(message = "출생연도는 필수입니다.")
    @Pattern(regexp = "^\\d{4}$", message = "출생연도는 숫자 4자리여야 합니다.")
    String birthYear,

    @NotBlank(message = "학번은 필수입니다.")
    @Pattern(regexp = "^\\d{10}$", message = "학번은 숫자 10자리여야 합니다.")
    String studentNumber,

    @NotNull(message = "학과는 필수입니다.")
    Major major
) {
}
