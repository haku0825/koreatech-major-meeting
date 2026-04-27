package haku.kmm.org.koreatechmajormeeting.domain.user;

import tools.jackson.databind.ObjectMapper;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.EmailVerificationRepository;
import haku.kmm.org.koreatechmajormeeting.support.IntegrationTestHelper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Test
    void signupFailsWhenPasswordConfirmDoesNotMatch() throws Exception {
        mockMvc.perform(
                post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "email", "mismatch@koreatech.ac.kr",
                                "password", "password1234",
                                "passwordConfirm", "password9999",
                                "name", "Mismatch",
                                "nickname", "mismatch",
                                "birthYear", "2002",
                                "studentNumber", "2026111100",
                                "major", "COMPUTER_SCIENCE"
                            )
                        )
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("USER_400_3"));
    }

    @Test
    void signupFailsWhenStudentNumberIsNotTenDigits() throws Exception {
        mockMvc.perform(
                post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "email", "bad-student@koreatech.ac.kr",
                                "password", "password1234",
                                "passwordConfirm", "password1234",
                                "name", "StudentLength",
                                "nickname", "student-length",
                                "birthYear", "2002",
                                "studentNumber", "20261111",
                                "major", "COMPUTER_SCIENCE"
                            )
                        )
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void signupAllowsLoginButBlocksProtectedFeaturesUntilFullyVerified() throws Exception {
        String email = "tester@koreatech.ac.kr";
        String password = "password1234";
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);

        String signupToken = helper.signup(email, password, "Tester", "2026123400", "COMPUTER_SCIENCE");
        assertThat(signupToken).isNotBlank();

        String accessToken = helper.login(email, password);
        assertThat(accessToken).isNotBlank();

        mockMvc.perform(
                get("/api/v1/posts")
                    .header("Authorization", "Bearer " + accessToken)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("AUTH_401_2"));

        helper.sendEmailCode(email);
        helper.verifyEmailCode(email);

        mockMvc.perform(
                get("/api/v1/posts")
                    .header("Authorization", "Bearer " + accessToken)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("AUTH_401_3"));

        Long studentCardRequestId = helper.uploadStudentCard(signupToken, "tester-card.jpg");

        String adminEmail = "admin@koreatech.ac.kr";
        String adminPassword = "adminpass1234";
        helper.signup(adminEmail, adminPassword, "Admin", "2026000000", "COMPUTER_SCIENCE");
        helper.sendEmailCode(adminEmail);
        helper.verifyEmailCode(adminEmail);
        helper.promoteAdmin(adminEmail);
        String adminToken = helper.login(adminEmail, adminPassword);
        helper.approveStudentCard(adminToken, studentCardRequestId);

        mockMvc.perform(
                get("/api/v1/users/me")
                    .header("Authorization", "Bearer " + accessToken)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value(email))
            .andExpect(jsonPath("$.data.name").value("Tester"))
            .andExpect(jsonPath("$.data.major").value("COMPUTER_SCIENCE"))
            .andExpect(jsonPath("$.data.emailVerified").value(true))
            .andExpect(jsonPath("$.data.studentCardVerified").value(true));
    }
}
