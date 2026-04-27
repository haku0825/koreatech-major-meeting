package haku.kmm.org.koreatechmajormeeting.domain.post;

import haku.kmm.org.koreatechmajormeeting.domain.chat.repository.ChatMessageRepository;
import haku.kmm.org.koreatechmajormeeting.domain.chat.repository.ChatRoomRepository;
import haku.kmm.org.koreatechmajormeeting.domain.chat.entity.ChatRoomStatus;
import haku.kmm.org.koreatechmajormeeting.domain.post.entity.PostStatus;
import haku.kmm.org.koreatechmajormeeting.domain.post.repository.PostRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PostDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Test
    void deletePost() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);
        String ownerStudentNumber = "2026999900";
        String accessToken = prepareApprovedUserToken(helper, "post-delete-owner@koreatech.ac.kr", ownerStudentNumber);

        helper.signup("del-member1@koreatech.ac.kr", "password1234", "DelMember1", "2024777100", "COMPUTER_SCIENCE");
        helper.signup("del-member2@koreatech.ac.kr", "password1234", "DelMember2", "2024777200", "DESIGN");

        MvcResult createResult = mockMvc.perform(
                post("/api/v1/posts")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Map.of(
                            "totalMemberCount", 2,
                            "introduction", "삭제 테스트 포스트",
                            "memberProfiles", List.of(
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", ownerStudentNumber),
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", "2024777100")
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
                delete("/api/v1/posts/{postId}", postId)
                    .header("Authorization", "Bearer " + accessToken)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        assertThat(postRepository.findById(postId)).isPresent();
        assertThat(postRepository.findById(postId).orElseThrow().getStatus()).isEqualTo(PostStatus.CLOSED);
    }

    @Test
    void deletePostClosesRelatedChatRoomsAndKeepsMessages() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);
        String ownerStudentNumber = "2026999000";
        String ownerToken = prepareApprovedUserToken(helper, "post-delete-chat-owner@koreatech.ac.kr", ownerStudentNumber);
        String requesterToken = prepareApprovedUserToken(helper, "post-delete-chat-requester@koreatech.ac.kr", "2026999100");

        helper.signup("delchat-member1@koreatech.ac.kr", "password1234", "DelChat1", "2024888100", "COMPUTER_SCIENCE");
        helper.signup("delchat-member2@koreatech.ac.kr", "password1234", "DelChat2", "2024888200", "MECHANICAL");

        MvcResult createResult = mockMvc.perform(
                post("/api/v1/posts")
                    .header("Authorization", "Bearer " + ownerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Map.of(
                            "totalMemberCount", 2,
                            "introduction", "삭제 시 채팅 정리 테스트",
                            "memberProfiles", List.of(
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", ownerStudentNumber),
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", "2024888100")
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

        MvcResult roomResult = mockMvc.perform(
                post("/api/v1/chats/rooms")
                    .header("Authorization", "Bearer " + requesterToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("postId", postId)))
            )
            .andExpect(status().isOk())
            .andReturn();

        Long roomId = objectMapper.readTree(roomResult.getResponse().getContentAsString())
            .path("data")
            .path("roomId")
            .asLong();

        mockMvc.perform(
                post("/api/v1/chats/rooms/{roomId}/messages", roomId)
                    .header("Authorization", "Bearer " + requesterToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("content", "삭제 전 메시지")))
            )
            .andExpect(status().isOk());

        assertThat(chatRoomRepository.findAll().stream().anyMatch(room -> room.getPostId().equals(postId))).isTrue();
        assertThat(chatMessageRepository.findAll().stream().anyMatch(message -> message.getRoom().getId().equals(roomId))).isTrue();

        mockMvc.perform(
                delete("/api/v1/posts/{postId}", postId)
                    .header("Authorization", "Bearer " + ownerToken)
            )
            .andExpect(status().isOk());

        assertThat(postRepository.findById(postId)).isPresent();
        assertThat(postRepository.findById(postId).orElseThrow().getStatus()).isEqualTo(PostStatus.CLOSED);
        assertThat(chatRoomRepository.findAll().stream().filter(room -> room.getPostId().equals(postId)).allMatch(
            room -> room.getStatus() == ChatRoomStatus.CLOSED
        )).isTrue();
        assertThat(chatMessageRepository.findAll().stream().anyMatch(message -> message.getRoom().getId().equals(roomId))).isTrue();
    }

    private String prepareApprovedUserToken(
        IntegrationTestHelper helper,
        String userEmail,
        String studentNumber
    ) throws Exception {
        String userPassword = "password1234";
        String signupToken = helper.signup(userEmail, userPassword, "DeleteOwner", studentNumber, "COMPUTER_SCIENCE");
        helper.sendEmailCode(userEmail);
        helper.verifyEmailCode(userEmail);
        Long requestId = helper.uploadStudentCard(signupToken, "delete-owner-card.jpg");

        String adminEmail = "admin-delete-" + studentNumber + "@koreatech.ac.kr";
        String adminPassword = "adminpass1234";
        helper.signup(adminEmail, adminPassword, "AdminDelete", "3030" + studentNumber.substring(4), "COMPUTER_SCIENCE");
        helper.sendEmailCode(adminEmail);
        helper.verifyEmailCode(adminEmail);
        helper.promoteAdmin(adminEmail);
        String adminToken = helper.login(adminEmail, adminPassword);
        helper.approveStudentCard(adminToken, requestId);

        return helper.login(userEmail, userPassword);
    }
}
