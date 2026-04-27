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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PostUpdateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Test
    void updatePost() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);
        String ownerStudentNumber = "2026888800";
        String accessToken = prepareApprovedUserToken(helper, "post-update-owner@koreatech.ac.kr", ownerStudentNumber);

        helper.signup("upd-member1@koreatech.ac.kr", "password1234", "UpdMember1", "2024120100", "COMPUTER_SCIENCE");
        helper.signup("upd-member2@koreatech.ac.kr", "password1234", "UpdMember2", "2024120200", "DESIGN");
        helper.signup("upd-member3@koreatech.ac.kr", "password1234", "UpdMember3", "2024999100", "COMPUTER_SCIENCE");
        helper.signup("upd-member4@koreatech.ac.kr", "password1234", "UpdMember4", "2024999200", "MECHANICAL");
        helper.signup("upd-member5@koreatech.ac.kr", "password1234", "UpdMember5", "2024999300", "INDUSTRIAL_MANAGEMENT");

        MvcResult createResult = mockMvc.perform(
                post("/api/v1/posts")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Map.of(
                            "totalMemberCount", 2,
                            "introduction", "수정 전 소개글",
                            "memberProfiles", List.of(
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", ownerStudentNumber),
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", "2024120100")
                            )
                        )
                    ))
            )
            .andExpect(status().isOk())
            .andReturn();

        Long postId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .path("data")
            .path("postId")
            .asLong();

        mockMvc.perform(
                put("/api/v1/posts/{postId}", postId)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Map.of(
                            "totalMemberCount", 3,
                            "introduction", "수정 후 소개글",
                            "memberProfiles", List.of(
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", ownerStudentNumber),
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", "2024999100"),
                                Map.of("major", "MECHANICAL", "studentNumber", "2024999200")
                            )
                        )
                    ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.postId").value(postId))
            .andExpect(jsonPath("$.data.totalMemberCount").value(3))
            .andExpect(jsonPath("$.data.introduction").value("수정 후 소개글"))
            .andExpect(jsonPath("$.data.memberProfiles.length()").value(3));
    }

    private String prepareApprovedUserToken(
        IntegrationTestHelper helper,
        String userEmail,
        String studentNumber
    ) throws Exception {
        String userPassword = "password1234";
        String signupToken = helper.signup(userEmail, userPassword, "UpdateOwner", studentNumber, "COMPUTER_SCIENCE");
        helper.sendEmailCode(userEmail);
        helper.verifyEmailCode(userEmail);
        Long requestId = helper.uploadStudentCard(signupToken, "update-owner-card.jpg");

        String adminEmail = "admin-update@koreatech.ac.kr";
        String adminPassword = "adminpass1234";
        helper.signup(adminEmail, adminPassword, "AdminUpdate", "2026000300", "COMPUTER_SCIENCE");
        helper.sendEmailCode(adminEmail);
        helper.verifyEmailCode(adminEmail);
        helper.promoteAdmin(adminEmail);
        String adminToken = helper.login(adminEmail, adminPassword);
        helper.approveStudentCard(adminToken, requestId);

        return helper.login(userEmail, userPassword);
    }
}
