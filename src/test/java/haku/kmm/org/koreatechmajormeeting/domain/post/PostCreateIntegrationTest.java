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
class PostCreateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Test
    void createPost() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);
        String ownerStudentNumber = "2026555500";
        String accessToken = prepareApprovedUserToken(helper, "post-owner@koreatech.ac.kr", ownerStudentNumber);

        helper.signup("member1@koreatech.ac.kr", "password1234", "Member1", "2024123400", "COMPUTER_SCIENCE");
        helper.signup("member2@koreatech.ac.kr", "password1234", "Member2", "2024123500", "MECHANICAL");

        mockMvc.perform(
                post("/api/v1/posts")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Map.of(
                            "totalMemberCount", 2,
                            "introduction", "멤버별 과/학번 입력 테스트 포스트",
                            "memberProfiles", List.of(
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", ownerStudentNumber),
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", "2024123400")
                            )
                        )
                    ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.postId").isNumber())
            .andExpect(jsonPath("$.data.totalMemberCount").value(2))
            .andExpect(jsonPath("$.data.memberProfiles.length()").value(2))
            .andExpect(jsonPath("$.data.memberProfiles[0].major").value("COMPUTER_SCIENCE"))
            .andExpect(jsonPath("$.data.status").value("RECRUITING"));
    }

    @Test
    void createPostFailsWhenSameWriterAlreadyHasPost() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);
        String ownerStudentNumber = "2026555600";
        String accessToken = prepareApprovedUserToken(helper, "post-owner-single@koreatech.ac.kr", ownerStudentNumber);

        helper.signup("single-member1@koreatech.ac.kr", "password1234", "SingleMember1", "2024223100", "COMPUTER_SCIENCE");
        helper.signup("single-member2@koreatech.ac.kr", "password1234", "SingleMember2", "2024223200", "MECHANICAL");
        helper.signup("single-member3@koreatech.ac.kr", "password1234", "SingleMember3", "2024223300", "DESIGN");

        mockMvc.perform(
                post("/api/v1/posts")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Map.of(
                            "totalMemberCount", 2,
                            "introduction", "첫 포스트",
                            "memberProfiles", List.of(
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", ownerStudentNumber),
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", "2024223100")
                            )
                        )
                    ))
            )
            .andExpect(status().isOk());

        mockMvc.perform(
                post("/api/v1/posts")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Map.of(
                            "totalMemberCount", 2,
                            "introduction", "두 번째 포스트",
                            "memberProfiles", List.of(
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", ownerStudentNumber),
                                Map.of("major", "DESIGN", "studentNumber", "2024223300")
                            )
                        )
                    ))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("POST_409_1"));
    }

    private String prepareApprovedUserToken(
        IntegrationTestHelper helper,
        String userEmail,
        String studentNumber
    ) throws Exception {
        String userPassword = "password1234";
        String signupToken = helper.signup(userEmail, userPassword, "PostOwner", studentNumber, "COMPUTER_SCIENCE");
        helper.sendEmailCode(userEmail);
        helper.verifyEmailCode(userEmail);
        Long requestId = helper.uploadStudentCard(signupToken, "owner-card.jpg");

        String adminEmail = "admin-create-" + studentNumber + "@koreatech.ac.kr";
        String adminPassword = "adminpass1234";
        helper.signup(adminEmail, adminPassword, "AdminCreate", "3030" + studentNumber.substring(4), "COMPUTER_SCIENCE");
        helper.sendEmailCode(adminEmail);
        helper.verifyEmailCode(adminEmail);
        helper.promoteAdmin(adminEmail);
        String adminToken = helper.login(adminEmail, adminPassword);
        helper.approveStudentCard(adminToken, requestId);

        return helper.login(userEmail, userPassword);
    }
}
