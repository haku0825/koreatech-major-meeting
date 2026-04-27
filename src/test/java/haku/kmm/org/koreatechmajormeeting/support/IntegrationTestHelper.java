package haku.kmm.org.koreatechmajormeeting.support;

import haku.kmm.org.koreatechmajormeeting.domain.user.entity.EmailVerification;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.EmailVerificationRepository;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
public class IntegrationTestHelper {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final EmailVerificationRepository emailVerificationRepository;

    public String signup(
        String email,
        String password,
        String name,
        String studentNumber,
        String major
    ) throws Exception {
        String nickname = email.contains("@") ? email.substring(0, email.indexOf('@')) : name;
        MvcResult result = mockMvc.perform(
                post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Map.of(
                            "email", email,
                            "password", password,
                            "passwordConfirm", password,
                            "name", name,
                            "nickname", nickname,
                            "birthYear", "2002",
                            "studentNumber", studentNumber,
                            "major", major
                        )
                    ))
            )
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
            .path("data")
            .path("accessToken")
            .asText();
    }

    public void sendEmailCode(String email) throws Exception {
        mockMvc.perform(
                post("/api/v1/auth/email/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("email", email)))
            )
            .andExpect(status().isOk());
    }

    public void verifyEmailCode(String email) throws Exception {
        EmailVerification verification = emailVerificationRepository.findByEmail(email).orElseThrow();
        mockMvc.perform(
                post("/api/v1/auth/email/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Map.of("email", email, "code", verification.getCode())
                    ))
            )
            .andExpect(status().isOk());
    }

    public String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        Map.of("email", email, "password", password)
                    ))
            )
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
            .path("data")
            .path("accessToken")
            .asText();
    }

    public Long uploadStudentCard(String accessToken, String fileName) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            fileName,
            "image/jpeg",
            "dummy-image".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult result = mockMvc.perform(
                multipart("/api/v1/users/me/student-card")
                    .file(file)
                    .header("Authorization", "Bearer " + accessToken)
            )
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
            .path("data")
            .path("requestId")
            .asLong();
    }

    public void promoteAdmin(String email) throws Exception {
        mockMvc.perform(
                post("/api/v1/dev/db/admin/promote")
                    .param("email", email)
            )
            .andExpect(status().isOk());
    }

    public void approveStudentCard(String adminToken, Long requestId) throws Exception {
        mockMvc.perform(
                post("/api/v1/admin/student-cards/{requestId}/approve", requestId)
                    .header("Authorization", "Bearer " + adminToken)
            )
            .andExpect(status().isOk());
    }
}
