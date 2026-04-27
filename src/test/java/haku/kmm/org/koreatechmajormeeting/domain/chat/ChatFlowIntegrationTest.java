package haku.kmm.org.koreatechmajormeeting.domain.chat;

import haku.kmm.org.koreatechmajormeeting.domain.user.repository.EmailVerificationRepository;
import haku.kmm.org.koreatechmajormeeting.support.IntegrationTestHelper;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Test
    void createRoomAndExchangeMessages() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);

        String ownerStudentNumber = "2026400100";
        String ownerToken = prepareApprovedUserToken(helper, "chat-owner@koreatech.ac.kr", ownerStudentNumber, "ChatOwner");
        String requesterToken = prepareApprovedUserToken(helper, "chat-requester@koreatech.ac.kr", "2026400200", "ChatRequester");

        helper.signup("chat-member1@koreatech.ac.kr", "password1234", "ChatMember1", "2040000100", "COMPUTER_SCIENCE");
        helper.signup("chat-member2@koreatech.ac.kr", "password1234", "ChatMember2", "2040000200", "MECHANICAL");

        Long postId = createPost(ownerToken, "채팅 테스트 포스트", ownerStudentNumber, "2040000200", "MECHANICAL");

        MvcResult roomResult = mockMvc.perform(
                post("/api/v1/chats/rooms")
                    .header("Authorization", "Bearer " + requesterToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("postId", postId)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.createdNew").value(true))
            .andReturn();

        Long roomId = objectMapper.readTree(roomResult.getResponse().getContentAsString())
            .path("data")
            .path("roomId")
            .asLong();

        MvcResult firstMessageResult = mockMvc.perform(
                post("/api/v1/chats/rooms/{roomId}/messages", roomId)
                    .header("Authorization", "Bearer " + requesterToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("content", "안녕하세요, 포스트 보고 연락드려요.")))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roomId").value(roomId))
            .andReturn();

        long firstMessageId = objectMapper.readTree(firstMessageResult.getResponse().getContentAsString())
            .path("data")
            .path("messageId")
            .asLong();

        MvcResult duplicateMessageResult = mockMvc.perform(
                post("/api/v1/chats/rooms/{roomId}/messages", roomId)
                    .header("Authorization", "Bearer " + requesterToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("content", "안녕하세요, 포스트 보고 연락드려요.")))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roomId").value(roomId))
            .andReturn();

        long duplicateMessageId = objectMapper.readTree(duplicateMessageResult.getResponse().getContentAsString())
            .path("data")
            .path("messageId")
            .asLong();
        assertThat(duplicateMessageId).isEqualTo(firstMessageId);

        mockMvc.perform(
                get("/api/v1/chats/rooms")
                    .header("Authorization", "Bearer " + ownerToken)
                    .param("page", "0")
                    .param("size", "10")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.rooms[0].roomId").value(roomId));

        mockMvc.perform(
                post("/api/v1/chats/rooms/{roomId}/messages", roomId)
                    .header("Authorization", "Bearer " + ownerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("content", "네 반갑습니다!")))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roomId").value(roomId));

        MvcResult messagesResult = mockMvc.perform(
                get("/api/v1/chats/rooms/{roomId}/messages", roomId)
                    .header("Authorization", "Bearer " + requesterToken)
                    .param("size", "30")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.messages.length()").value(2))
            .andReturn();

        JsonNode messages = objectMapper.readTree(messagesResult.getResponse().getContentAsString())
            .path("data")
            .path("messages");

        assertThat(messages.get(0).path("content").asText()).contains("안녕하세요");
        assertThat(messages.get(1).path("content").asText()).isEqualTo("네 반갑습니다!");
    }

    @Test
    void nonParticipantCannotSendMessage() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);

        String ownerStudentNumber = "2026410100";
        String ownerToken = prepareApprovedUserToken(helper, "chat-owner2@koreatech.ac.kr", ownerStudentNumber, "ChatOwner2");
        String requesterToken = prepareApprovedUserToken(helper, "chat-requester2@koreatech.ac.kr", "2026410200", "ChatRequester2");
        String outsiderToken = prepareApprovedUserToken(helper, "chat-outsider@koreatech.ac.kr", "2026410300", "ChatOutsider");

        helper.signup("chat-member3@koreatech.ac.kr", "password1234", "ChatMember3", "2040000300", "COMPUTER_SCIENCE");
        helper.signup("chat-member4@koreatech.ac.kr", "password1234", "ChatMember4", "2040000400", "MECHANICAL");

        Long postId = createPost(ownerToken, "채팅 권한 테스트 포스트", ownerStudentNumber, "2040000400", "MECHANICAL");
        Long roomId = createRoom(requesterToken, postId);

        mockMvc.perform(
                post("/api/v1/chats/rooms/{roomId}/messages", roomId)
                    .header("Authorization", "Bearer " + outsiderToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("content", "외부 사용자 메시지")))
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("CHAT_403_1"));
    }

    @Test
    void postMemberCannotCreateRoomForSamePost() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);

        String ownerStudentNumber = "2026420100";
        String ownerToken = prepareApprovedUserToken(helper, "chat-owner-member@koreatech.ac.kr", ownerStudentNumber, "ChatOwnerMember");
        String memberToken = prepareApprovedUserToken(helper, "chat-member-user@koreatech.ac.kr", "2026420200", "ChatMemberUser");

        helper.signup("chat-member9@koreatech.ac.kr", "password1234", "ChatMember9", "2040000900", "MECHANICAL");

        Long postId = createPost(ownerToken, "멤버 본인 쪽지 금지 테스트", ownerStudentNumber, "2026420200", "COMPUTER_SCIENCE");

        mockMvc.perform(
                post("/api/v1/chats/rooms")
                    .header("Authorization", "Bearer " + memberToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("postId", postId)))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("CHAT_400_2"));
    }

    @Test
    void postWriterCanCloseRoomAndPostThenAllRoomDataIsClosed() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);

        String ownerStudentNumber = "2026411100";
        String ownerToken = prepareApprovedUserToken(helper, "chat-owner3@koreatech.ac.kr", ownerStudentNumber, "ChatOwner3");
        String requesterToken = prepareApprovedUserToken(helper, "chat-requester3@koreatech.ac.kr", "2026411200", "ChatRequester3");

        helper.signup("chat-member5@koreatech.ac.kr", "password1234", "ChatMember5", "2040000500", "COMPUTER_SCIENCE");
        helper.signup("chat-member6@koreatech.ac.kr", "password1234", "ChatMember6", "2040000600", "MECHANICAL");

        Long postId = createPost(ownerToken, "채팅방 삭제 테스트 포스트", ownerStudentNumber, "2040000600", "MECHANICAL");
        Long roomId = createRoom(requesterToken, postId);

        mockMvc.perform(
                post("/api/v1/chats/rooms/{roomId}/messages", roomId)
                    .header("Authorization", "Bearer " + requesterToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("content", "삭제 전 메시지")))
            )
            .andExpect(status().isOk());

        mockMvc.perform(
                delete("/api/v1/chats/rooms/{roomId}", roomId)
                    .header("Authorization", "Bearer " + ownerToken)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        MvcResult postsResult = mockMvc.perform(
                get("/api/v1/posts")
                    .header("Authorization", "Bearer " + requesterToken)
                    .param("page", "0")
                    .param("size", "10")
            )
            .andExpect(status().isOk())
            .andReturn();

        JsonNode posts = objectMapper.readTree(postsResult.getResponse().getContentAsString())
            .path("data")
            .path("posts");
        boolean containsDeletedPost = StreamSupport.stream(posts.spliterator(), false)
            .anyMatch(node -> node.path("postId").asLong() == postId);
        assertThat(containsDeletedPost).isFalse();

        MvcResult roomsResult = mockMvc.perform(
                get("/api/v1/chats/rooms")
                    .header("Authorization", "Bearer " + requesterToken)
                    .param("page", "0")
                    .param("size", "10")
            )
            .andExpect(status().isOk())
            .andReturn();

        JsonNode rooms = objectMapper.readTree(roomsResult.getResponse().getContentAsString())
            .path("data")
            .path("rooms");
        boolean closedRoom = StreamSupport.stream(rooms.spliterator(), false)
            .anyMatch(node -> node.path("roomId").asLong() == roomId);
        assertThat(closedRoom).isTrue();

        mockMvc.perform(
                get("/api/v1/chats/rooms/{roomId}/messages", roomId)
                    .header("Authorization", "Bearer " + requesterToken)
                    .param("size", "30")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.messages.length()").value(1));

        mockMvc.perform(
                post("/api/v1/chats/rooms/{roomId}/messages", roomId)
                    .header("Authorization", "Bearer " + requesterToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("content", "종료 후 메시지")))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("CHAT_400_4"));
    }

    @Test
    void requesterCannotCloseRoom() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);

        String ownerStudentNumber = "2026412100";
        String ownerToken = prepareApprovedUserToken(helper, "chat-owner4@koreatech.ac.kr", ownerStudentNumber, "ChatOwner4");
        String requesterToken = prepareApprovedUserToken(helper, "chat-requester4@koreatech.ac.kr", "2026412200", "ChatRequester4");

        helper.signup("chat-member7@koreatech.ac.kr", "password1234", "ChatMember7", "2040000700", "COMPUTER_SCIENCE");
        helper.signup("chat-member8@koreatech.ac.kr", "password1234", "ChatMember8", "2040000800", "MECHANICAL");

        Long postId = createPost(ownerToken, "채팅방 종료 권한 테스트", ownerStudentNumber, "2040000800", "MECHANICAL");
        Long roomId = createRoom(requesterToken, postId);

        mockMvc.perform(
                delete("/api/v1/chats/rooms/{roomId}", roomId)
                    .header("Authorization", "Bearer " + requesterToken)
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("CHAT_403_1"));
    }

    @Test
    void requesterLeaveClosesOnlyRoomAndKeepsPostRecruiting() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);

        String ownerStudentNumber = "2026413100";
        String ownerToken = prepareApprovedUserToken(helper, "chat-owner5@koreatech.ac.kr", ownerStudentNumber, "ChatOwner5");
        String requesterToken = prepareApprovedUserToken(helper, "chat-requester5@koreatech.ac.kr", "2026413200", "ChatRequester5");

        helper.signup("chat-member10@koreatech.ac.kr", "password1234", "ChatMember10", "2040001000", "MECHANICAL");

        Long postId = createPost(ownerToken, "채팅방 나가기 테스트", ownerStudentNumber, "2040001000", "MECHANICAL");
        Long roomId = createRoom(requesterToken, postId);

        mockMvc.perform(
                delete("/api/v1/chats/rooms/{roomId}/leave", roomId)
                    .header("Authorization", "Bearer " + requesterToken)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        MvcResult roomsResult = mockMvc.perform(
                get("/api/v1/chats/rooms")
                    .header("Authorization", "Bearer " + requesterToken)
                    .param("page", "0")
                    .param("size", "10")
            )
            .andExpect(status().isOk())
            .andReturn();

        JsonNode rooms = objectMapper.readTree(roomsResult.getResponse().getContentAsString())
            .path("data")
            .path("rooms");
        boolean hasClosedRoom = StreamSupport.stream(rooms.spliterator(), false)
            .anyMatch(node -> node.path("roomId").asLong() == roomId && "CLOSED".equals(node.path("status").asText()));
        assertThat(hasClosedRoom).isTrue();

        MvcResult postsResult = mockMvc.perform(
                get("/api/v1/posts")
                    .header("Authorization", "Bearer " + requesterToken)
                    .param("page", "0")
                    .param("size", "10")
            )
            .andExpect(status().isOk())
            .andReturn();

        JsonNode posts = objectMapper.readTree(postsResult.getResponse().getContentAsString())
            .path("data")
            .path("posts");
        JsonNode postNode = StreamSupport.stream(posts.spliterator(), false)
            .filter(node -> node.path("postId").asLong() == postId)
            .findFirst()
            .orElse(null);
        assertThat(postNode).isNotNull();
        assertThat(postNode.path("status").asText()).isEqualTo("RECRUITING");

        mockMvc.perform(
                get("/api/v1/chats/rooms/{roomId}/messages", roomId)
                    .header("Authorization", "Bearer " + requesterToken)
                    .param("size", "30")
            )
            .andExpect(status().isOk());

        mockMvc.perform(
                post("/api/v1/chats/rooms/{roomId}/messages", roomId)
                    .header("Authorization", "Bearer " + requesterToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("content", "나간 뒤 전송")))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("CHAT_400_4"));
    }

    @Test
    void postIsHiddenFromFeedWhileChatIsInProgress() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);

        String ownerStudentNumber = "2026415100";
        String ownerToken = prepareApprovedUserToken(helper, "chat-owner7@koreatech.ac.kr", ownerStudentNumber, "ChatOwner7");
        String requesterToken = prepareApprovedUserToken(helper, "chat-requester7@koreatech.ac.kr", "2026415200", "ChatRequester7");
        String viewerToken = prepareApprovedUserToken(helper, "chat-viewer7@koreatech.ac.kr", "2026415300", "ChatViewer7");

        helper.signup("chat-member12@koreatech.ac.kr", "password1234", "ChatMember12", "2040001200", "MECHANICAL");

        Long postId = createPost(ownerToken, "채팅 중 피드 숨김 테스트", ownerStudentNumber, "2040001200", "MECHANICAL");

        MvcResult beforeChatPostsResult = mockMvc.perform(
                get("/api/v1/posts")
                    .header("Authorization", "Bearer " + viewerToken)
                    .param("page", "0")
                    .param("size", "20")
            )
            .andExpect(status().isOk())
            .andReturn();

        JsonNode beforeChatPosts = objectMapper.readTree(beforeChatPostsResult.getResponse().getContentAsString())
            .path("data")
            .path("posts");
        boolean beforeContainsPost = StreamSupport.stream(beforeChatPosts.spliterator(), false)
            .anyMatch(node -> node.path("postId").asLong() == postId);
        assertThat(beforeContainsPost).isTrue();

        createRoom(requesterToken, postId);

        MvcResult duringChatPostsResult = mockMvc.perform(
                get("/api/v1/posts")
                    .header("Authorization", "Bearer " + viewerToken)
                    .param("page", "0")
                    .param("size", "20")
            )
            .andExpect(status().isOk())
            .andReturn();

        JsonNode duringChatPosts = objectMapper.readTree(duringChatPostsResult.getResponse().getContentAsString())
            .path("data")
            .path("posts");
        boolean duringContainsPost = StreamSupport.stream(duringChatPosts.spliterator(), false)
            .anyMatch(node -> node.path("postId").asLong() == postId);
        assertThat(duringContainsPost).isFalse();
    }

    @Test
    void writerCannotLeaveRoom() throws Exception {
        IntegrationTestHelper helper = new IntegrationTestHelper(mockMvc, objectMapper, emailVerificationRepository);

        String ownerStudentNumber = "2026414100";
        String ownerToken = prepareApprovedUserToken(helper, "chat-owner6@koreatech.ac.kr", ownerStudentNumber, "ChatOwner6");
        String requesterToken = prepareApprovedUserToken(helper, "chat-requester6@koreatech.ac.kr", "2026414200", "ChatRequester6");

        helper.signup("chat-member11@koreatech.ac.kr", "password1234", "ChatMember11", "2040001100", "MECHANICAL");

        Long postId = createPost(ownerToken, "작성자 나가기 금지 테스트", ownerStudentNumber, "2040001100", "MECHANICAL");
        Long roomId = createRoom(requesterToken, postId);

        mockMvc.perform(
                delete("/api/v1/chats/rooms/{roomId}/leave", roomId)
                    .header("Authorization", "Bearer " + ownerToken)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("CHAT_400_3"));
    }

    private Long createPost(
        String ownerToken,
        String introduction,
        String firstStudentNumber,
        String secondStudentNumber,
        String secondMajor
    ) throws Exception {
        MvcResult postResult = mockMvc.perform(
                post("/api/v1/posts")
                    .header("Authorization", "Bearer " + ownerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Map.of(
                            "totalMemberCount", 2,
                            "introduction", introduction,
                            "memberProfiles", List.of(
                                Map.of("major", "COMPUTER_SCIENCE", "studentNumber", firstStudentNumber),
                                Map.of("major", secondMajor, "studentNumber", secondStudentNumber)
                            )
                        )
                    ))
            )
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(postResult.getResponse().getContentAsString())
            .path("data")
            .path("postId")
            .asLong();
    }

    private Long createRoom(String requesterToken, Long postId) throws Exception {
        MvcResult roomResult = mockMvc.perform(
                post("/api/v1/chats/rooms")
                    .header("Authorization", "Bearer " + requesterToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("postId", postId)))
            )
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(roomResult.getResponse().getContentAsString())
            .path("data")
            .path("roomId")
            .asLong();
    }

    private String prepareApprovedUserToken(
        IntegrationTestHelper helper,
        String userEmail,
        String studentNumber,
        String name
    ) throws Exception {
        String userPassword = "password1234";
        String signupToken = helper.signup(userEmail, userPassword, name, studentNumber, "COMPUTER_SCIENCE");
        helper.sendEmailCode(userEmail);
        helper.verifyEmailCode(userEmail);
        Long requestId = helper.uploadStudentCard(signupToken, name + "-card.jpg");

        String adminEmail = "admin-" + studentNumber + "@koreatech.ac.kr";
        String adminPassword = "adminpass1234";
        helper.signup(adminEmail, adminPassword, "Admin" + studentNumber, "3030" + studentNumber.substring(4), "COMPUTER_SCIENCE");
        helper.sendEmailCode(adminEmail);
        helper.verifyEmailCode(adminEmail);
        helper.promoteAdmin(adminEmail);
        String adminToken = helper.login(adminEmail, adminPassword);
        helper.approveStudentCard(adminToken, requestId);

        return helper.login(userEmail, userPassword);
    }
}
