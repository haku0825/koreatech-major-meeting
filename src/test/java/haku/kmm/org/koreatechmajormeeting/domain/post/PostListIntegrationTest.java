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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PostListIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Test
    void listRecruitingPosts() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);
        String ownerStudentNumber = "2026777700";
        String accessToken = prepareApprovedUserToken(helper, "post-list-owner@koreatech.ac.kr", ownerStudentNumber);

        helper.signup("list-member1@koreatech.ac.kr", "password1234", "ListMember1", "2024133100", "COMPUTER_SCIENCE");
        helper.signup("list-member2@koreatech.ac.kr", "password1234", "ListMember2", "2024133200", "DESIGN");

        mockMvc.perform(
                post("/api/v1/posts")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Map.of(
                            "totalMemberCount", 2,
                            "introduction", "목록 조회 테스트용 포스트",
                            "memberProfiles", List.of(
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", ownerStudentNumber),
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", "2024133100")
                            )
                        )
                    ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.postId").isNumber());

        MvcResult listResult = mockMvc.perform(
                get("/api/v1/posts")
                    .header("Authorization", "Bearer " + accessToken)
                    .param("page", "0")
                    .param("size", "10")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.posts").isArray())
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(10))
            .andReturn();

        JsonNode listJson = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertThat(listJson.path("data").path("posts").size()).isGreaterThanOrEqualTo(1);
        JsonNode firstPost = listJson.path("data").path("posts").get(0);
        assertThat(firstPost.path("status").asText()).isEqualTo("RECRUITING");
        assertThat(firstPost.path("memberProfiles").size()).isGreaterThanOrEqualTo(1);
    }

    private String prepareApprovedUserToken(
        IntegrationTestHelper helper,
        String userEmail,
        String studentNumber
    ) throws Exception {
        String userPassword = "password1234";
        String signupToken = helper.signup(userEmail, userPassword, "ListOwner", studentNumber, "COMPUTER_SCIENCE");
        helper.sendEmailCode(userEmail);
        helper.verifyEmailCode(userEmail);
        Long requestId = helper.uploadStudentCard(signupToken, "list-owner-card.jpg");

        String adminEmail = "admin-list@koreatech.ac.kr";
        String adminPassword = "adminpass1234";
        helper.signup(adminEmail, adminPassword, "AdminList", "2026000200", "COMPUTER_SCIENCE");
        helper.sendEmailCode(adminEmail);
        helper.verifyEmailCode(adminEmail);
        helper.promoteAdmin(adminEmail);
        String adminToken = helper.login(adminEmail, adminPassword);
        helper.approveStudentCard(adminToken, requestId);

        return helper.login(userEmail, userPassword);
    }
}
