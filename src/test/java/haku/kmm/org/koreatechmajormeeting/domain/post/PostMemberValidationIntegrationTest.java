package haku.kmm.org.koreatechmajormeeting.domain.post;

import haku.kmm.org.koreatechmajormeeting.domain.user.repository.EmailVerificationRepository;
import haku.kmm.org.koreatechmajormeeting.support.IntegrationTestHelper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PostMemberValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Test
    void createPostFailsWhenMemberStudentNumberNotFound() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);
        String ownerStudentNumber = "2026111100";
        String accessToken = prepareApprovedUserToken(helper, "post-validate-owner@koreatech.ac.kr", ownerStudentNumber);

        mockMvc.perform(
                post("/api/v1/posts")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Map.of(
                            "totalMemberCount", 2,
                            "introduction", "멤버 검증 실패 테스트",
                            "memberProfiles", List.of(
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", ownerStudentNumber),
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", "2099999900")
                            )
                        )
                    ))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("POST_400_2"));
    }

    @Test
    void createPostFailsWhenMemberMajorMismatch() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);
        String ownerStudentNumber = "2026111200";
        String accessToken = prepareApprovedUserToken(helper, "post-validate-owner2@koreatech.ac.kr", ownerStudentNumber);

        helper.signup("major-check@koreatech.ac.kr", "password1234", "MajorCheck", "2024555500", "MECHANICAL");

        mockMvc.perform(
                post("/api/v1/posts")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Map.of(
                            "totalMemberCount", 2,
                            "introduction", "멤버 학과 불일치 테스트",
                            "memberProfiles", List.of(
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", ownerStudentNumber),
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", "2024555500")
                            )
                        )
                    ))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("POST_400_3"));
    }

    @Test
    void createPostFailsWhenMemberStudentNumberDuplicated() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);
        String ownerStudentNumber = "2026111300";
        String accessToken = prepareApprovedUserToken(helper, "post-validate-owner3@koreatech.ac.kr", ownerStudentNumber);

        mockMvc.perform(
                post("/api/v1/posts")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Map.of(
                            "totalMemberCount", 2,
                            "introduction", "멤버 학번 중복 테스트",
                            "memberProfiles", List.of(
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", ownerStudentNumber),
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", ownerStudentNumber)
                            )
                        )
                    ))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("POST_400_4"));
    }

    @Test
    void createPostFailsWhenWriterStudentNumberNotIncluded() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);
        String ownerStudentNumber = "2026111400";
        String accessToken = prepareApprovedUserToken(helper, "post-validate-owner4@koreatech.ac.kr", ownerStudentNumber);

        helper.signup("writer-check-member@koreatech.ac.kr", "password1234", "WriterCheckMember", "2024555600", "MECHANICAL");

        mockMvc.perform(
                post("/api/v1/posts")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Map.of(
                            "totalMemberCount", 1,
                            "introduction", "작성자 미포함 테스트",
                            "memberProfiles", List.of(
                                Map.of("major", "MECHANICAL", "studentNumber", "2024555600")
                            )
                        )
                    ))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("POST_400_5"));
    }

    private String prepareApprovedUserToken(
        IntegrationTestHelper helper,
        String userEmail,
        String studentNumber
    ) throws Exception {
        String userPassword = "password1234";
        String signupToken = helper.signup(userEmail, userPassword, "ValidateOwner", studentNumber, "COMPUTER_SCIENCE");
        helper.sendEmailCode(userEmail);
        helper.verifyEmailCode(userEmail);
        Long requestId = helper.uploadStudentCard(signupToken, "validate-owner-card.jpg");

        String adminEmail = "admin-validate" + studentNumber + "@koreatech.ac.kr";
        String adminPassword = "adminpass1234";
        helper.signup(adminEmail, adminPassword, "AdminValidate", "3030" + studentNumber.substring(4), "COMPUTER_SCIENCE");
        helper.sendEmailCode(adminEmail);
        helper.verifyEmailCode(adminEmail);
        helper.promoteAdmin(adminEmail);
        String adminToken = helper.login(adminEmail, adminPassword);
        helper.approveStudentCard(adminToken, requestId);

        return helper.login(userEmail, userPassword);
    }
}
