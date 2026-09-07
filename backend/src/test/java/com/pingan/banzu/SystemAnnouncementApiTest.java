package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemAnnouncementApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void publishesAndWithdrawsAnnouncementWithFrozenDeliveries() throws Exception {
    String token = login();
    JsonNode created = json(
        mockMvc.perform(
                post("/api/system/announcements")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "title": "全员安全提醒",
                          "content": "请及时完成本周安全学习。",
                          "severity": "IMPORTANT",
                          "audienceType": "ALL"
                        }
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
    String id = created.path("data").path("id").asText();

    JsonNode published = json(
        mockMvc.perform(
                post("/api/system/announcements/{id}/publish", id)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
    assertThat(published.path("data").path("status").asText()).isEqualTo("PUBLISHED");
    assertThat(published.path("data").path("deliveryCount").asLong()).isGreaterThan(0);

    JsonNode deliveries = json(
        mockMvc.perform(
                get("/api/system/notification-deliveries")
                    .param("announcementId", id)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
    assertThat(deliveries.path("data").path("total").asLong()).isGreaterThan(0);

    JsonNode withdrawn = json(
        mockMvc.perform(
                post("/api/system/announcements/{id}/withdraw", id)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
    assertThat(withdrawn.path("data").path("status").asText()).isEqualTo("WITHDRAWN");
  }

  private String login() throws Exception {
    JsonNode response = json(
        mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"admin\",\"password\":\"SAFE_TEST_PASSWORD\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
    return response.path("data").path("accessToken").asText();
  }

  private JsonNode json(String value) throws Exception {
    return objectMapper.readTree(value);
  }
}

