package haku.kmm.org.koreatechmajormeeting.domain.user;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import haku.kmm.org.koreatechmajormeeting.domain.user.entity.EmailVerification;
import haku.kmm.org.koreatechmajormeeting.domain.user.repository.EmailVerificationRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
    void emailVerificationSignupLoginAndMeFlow() throws Exception {
        String email = "tester@koreatech.ac.kr";

        mockMvc.perform(
                post("/api/v1/auth/email/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("email", email)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        EmailVerification emailVerification = emailVerificationRepository.findByEmail(email)
            .orElseThrow();

        mockMvc.perform(
                post("/api/v1/auth/email/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of("email", email, "code", emailVerification.getCode())
                        )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        MvcResult signupResult = mockMvc.perform(
                post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "email", email,
                                "password", "password1234",
                                "name", "Tester",
                                "studentNumber", "20261234",
                                "major", "COMPUTER_SCIENCE"
                            )
                        )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andReturn();

        JsonNode signupJson = objectMapper.readTree(signupResult.getResponse().getContentAsString());
        String accessToken = signupJson.path("data").path("accessToken").asText();
        assertThat(accessToken).isNotBlank();

        mockMvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of("email", email, "password", "password1234")
                        )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        mockMvc.perform(
                get("/api/v1/users/me")
                    .header("Authorization", "Bearer " + accessToken)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value(email))
            .andExpect(jsonPath("$.data.name").value("Tester"))
            .andExpect(jsonPath("$.data.major").value("COMPUTER_SCIENCE"));
    }
}
