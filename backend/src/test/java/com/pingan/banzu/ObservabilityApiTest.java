package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingan.banzu.config.RequestCorrelationFilter;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class ObservabilityApiTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @Test
  void exposesMinimalAnonymousLivenessAndReadiness() throws Exception {
    JsonNode liveness =
        objectMapper.readTree(
            mockMvc
                .perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray());
    JsonNode readiness =
        objectMapper.readTree(
            mockMvc
                .perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray());

    assertThat(liveness.fieldNames()).toIterable().containsExactly("status");
    assertThat(readiness.fieldNames()).toIterable().containsExactly("status");
    assertThat(liveness.path("status").asText()).isEqualTo("UP");
    assertThat(readiness.path("status").asText()).isEqualTo("UP");
  }

  @Test
  void protectsPrometheusMetricsWithAnAdminToken() throws Exception {
    mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());

    String metrics =
        mockMvc
            .perform(
                get("/actuator/prometheus")
                    .header("Authorization", "Bearer " + loginAsAdmin()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(metrics)
        .contains("jvm_memory_used_bytes")
        .contains("http_server_requests_seconds")
        .doesNotContain("PINGAN_JWT_SECRET", "PINGAN_DB_PASSWORD", "PINGAN_MINIO_SECRET_KEY");
  }

  @Test
  void preservesSafeRequestIdsAndReplacesUnsafeValues() throws Exception {
    String safeRequestId = "mini-submit:trace-001";
    mockMvc
        .perform(
            get("/actuator/health/liveness")
                .header(RequestCorrelationFilter.HEADER_NAME, safeRequestId))
        .andExpect(status().isOk())
        .andExpect(header().string(RequestCorrelationFilter.HEADER_NAME, safeRequestId));

    String generated =
        mockMvc
            .perform(
                get("/actuator/health/liveness")
                    .header(RequestCorrelationFilter.HEADER_NAME, "unsafe request id"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader(RequestCorrelationFilter.HEADER_NAME);
    assertThat(generated).matches("[0-9a-f-]{36}").isNotEqualTo("unsafe request id");
  }

  private String loginAsAdmin() throws Exception {
    JsonNode response =
        objectMapper.readTree(
            mockMvc
                .perform(
                    post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsBytes(
                                Map.of("username", "admin", "password", "123456"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray());
    return response.path("data").path("accessToken").asText();
  }
}
