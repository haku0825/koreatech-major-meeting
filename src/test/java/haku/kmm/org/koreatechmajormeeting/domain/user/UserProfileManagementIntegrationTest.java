package haku.kmm.org.koreatechmajormeeting.domain.user;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.WithdrawnUser;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.EmailVerificationRepository;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.WithdrawnUserRepository;
import haku.kmm.org.koreatechmajormeeting.support.IntegrationTestHelper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class UserProfileManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private WithdrawnUserRepository withdrawnUserRepository;

    @Test
    void updateAndDeleteMyAccount() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);
        String accessToken = prepareApprovedUserToken(helper, "a", "2026222200");

        mockMvc.perform(
                put("/api/v1/users/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Map.of(
                            "name", "UpdatedUser",
                            "nickname", "updated-nick",
                            "major", "DESIGN"
                        )
                    ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("UpdatedUser"))
            .andExpect(jsonPath("$.data.nickname").value("updated-nick"))
            .andExpect(jsonPath("$.data.major").value("DESIGN"));

        mockMvc.perform(
                delete("/api/v1/users/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Map.of(
                            "password", "password1234",
                            "reason", "테스트를 위한 탈퇴"
                        )
                    ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        WithdrawnUser withdrawnUser = withdrawnUserRepository.findAll().stream()
            .findFirst()
            .orElseThrow();
        assertThat(withdrawnUser.getStudentNumber()).isEqualTo("2026222200");
        assertThat(withdrawnUser.getName()).isEqualTo("UpdatedUser");
        assertThat(withdrawnUser.getReason()).isEqualTo("테스트를 위한 탈퇴");

        mockMvc.perform(
                get("/api/v1/users/me")
                    .header("Authorization", "Bearer " + accessToken)
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("USER_404_1"));
    }

    @Test
    void deleteMyAccountFailsWhenPasswordIsWrong() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);
        String accessToken = prepareApprovedUserToken(helper, "b", "2026222300");

        mockMvc.perform(
                delete("/api/v1/users/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Map.of(
                            "password", "wrong-password",
                            "reason", "잘못된 비밀번호 테스트"
                        )
                    ))
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("USER_401_1"));
    }

    private String prepareApprovedUserToken(
        IntegrationTestHelper helper,
        String suffix,
        String studentNumber
    ) throws Exception {
        String userEmail = "profile-user-" + suffix + "@koreatech.ac.kr";
        String userPassword = "password1234";
        String signupToken = helper.signup(userEmail, userPassword, "ProfileUser", studentNumber, "COMPUTER_SCIENCE");
        helper.sendEmailCode(userEmail);
        helper.verifyEmailCode(userEmail);
        Long requestId = helper.uploadStudentCard(signupToken, "profile-card.jpg");

        String adminEmail = "admin-profile-" + suffix + "@koreatech.ac.kr";
        String adminPassword = "adminpass1234";
        helper.signup(adminEmail, adminPassword, "AdminProfile", "3026" + studentNumber.substring(4), "COMPUTER_SCIENCE");
        helper.sendEmailCode(adminEmail);
        helper.verifyEmailCode(adminEmail);
        helper.promoteAdmin(adminEmail);
        String adminToken = helper.login(adminEmail, adminPassword);
        helper.approveStudentCard(adminToken, requestId);

        return helper.login(userEmail, userPassword);
    }
}
