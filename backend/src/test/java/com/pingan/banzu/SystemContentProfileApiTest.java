package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class SystemContentProfileApiTest {

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("delete from biz_attachment where biz_type = 'SYS_ORG_CONTENT_PROFILE'");
    jdbcTemplate.update("delete from sys_org_content_profile where org_id in (3, 4, 24)");
  }

  @Test
  void managesContentProfilesAndRejectsDuplicateOrganizations() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");

    JsonNode created =
        postJson(
                "/api/system/content-profiles",
                token,
                payload(3L, "Demo Works Company简介", "幕墙安全生产样板", "幕墙公司图文介绍", "幕墙宣传片", 20, "ACTIVE"))
            .path("data");

    long id = created.path("id").asLong();
    assertThat(created.path("orgId").asLong()).isEqualTo(3L);
    assertThat(created.path("orgName").asText()).isEqualTo("Demo Works Company");
    assertThat(created.path("title").asText()).isEqualTo("Demo Works Company简介");
    assertThat(created.path("status").asText()).isEqualTo("ACTIVE");

    mockMvc
        .perform(
            post("/api/system/content-profiles")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload(3L, "重复简介", "", "", "", 1, "ACTIVE"))))
        .andExpect(status().isBadRequest());

    JsonNode updated =
        putJson(
                "/api/system/content-profiles/" + id,
                token,
                payload(3L, "Demo Works Company更新", "更新副标题", "更新后的简介正文", "更新宣传片", 5, "DRAFT"))
            .path("data");
    assertThat(updated.path("title").asText()).isEqualTo("Demo Works Company更新");
    assertThat(updated.path("videoSortOrder").asInt()).isEqualTo(5);
    assertThat(updated.path("status").asText()).isEqualTo("DRAFT");

    JsonNode active =
        patchJson("/api/system/content-profiles/" + id + "/status", token, Map.of("status", "ACTIVE")).path("data");
    assertThat(active.path("status").asText()).isEqualTo("ACTIVE");

    JsonNode list = getJson("/api/system/content-profiles?keyword=更新&status=ACTIVE", token).path("data");
    assertThat(list.path("total").asInt()).isEqualTo(1);
    assertThat(list.path("items").get(0).path("id").asLong()).isEqualTo(id);

    mockMvc.perform(delete("/api/system/content-profiles/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    assertThat(
            jdbcTemplate.queryForObject("select deleted from sys_org_content_profile where id = ?", Long.class, id))
        .isEqualTo(id);
    assertThat(getJson("/api/system/content-profiles?keyword=更新", token).path("data").path("total").asInt())
        .isZero();

    JsonNode recreated =
        postJson(
                "/api/system/content-profiles",
                token,
                payload(3L, "Demo Works Company重新配置", "", "删除后重新创建", "", 30, "DRAFT"))
            .path("data");
    assertThat(recreated.path("orgId").asLong()).isEqualTo(3L);
  }

  @Test
  void uploadsImagesAndVideosAndReadApisReturnOnlyActiveContent() throws Exception {
    String token = login("admin", "SAFE_TEST_PASSWORD");
    long inactiveId =
        postJson(
                "/api/system/content-profiles",
                token,
                payload(24L, "Demo Company简介", "", "未启用内容", "资源宣传片", 1, "DRAFT"))
            .path("data")
            .path("id")
            .asLong();
    long activeId =
        postJson(
                "/api/system/content-profiles",
                token,
                payload(4L, "Demo Works Company简介", "Safety Operations Team", "Demo Works图文介绍", "Demo Works宣传片", 7, "ACTIVE"))
            .path("data")
            .path("id")
            .asLong();

    MockMultipartFile image = new MockMultipartFile("file", "简介.png", "image/png", new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
    JsonNode imageUploaded =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        multipart("/api/system/content-profiles/" + activeId + "/image")
                            .file(image)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");
    assertThat(imageUploaded.path("imageAttachment").path("fileKind").asText()).isEqualTo("IMAGE");
    assertThat(imageUploaded.path("imageAttachment").path("originalName").asText()).isEqualTo("简介.png");

    MockMultipartFile video =
        new MockMultipartFile("file", "宣传片.mp4", "video/mp4", new byte[] {0, 0, 0, 12, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'});
    JsonNode videoUploaded =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        multipart("/api/system/content-profiles/" + activeId + "/video")
                            .file(video)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8))
            .path("data");
    assertThat(videoUploaded.path("videoAttachment").path("fileKind").asText()).isEqualTo("VIDEO");
    assertThat(videoUploaded.path("videoAttachment").path("url").asText()).contains("/api/attachments/");

    mockMvc
        .perform(
            multipart("/api/system/content-profiles/" + activeId + "/image")
                .file(new MockMultipartFile("file", "错误.txt", "text/plain", "bad".getBytes(StandardCharsets.UTF_8)))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            multipart("/api/system/content-profiles/" + activeId + "/video")
                .file(new MockMultipartFile("file", "错误.png", "image/png", new byte[] {1}))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());

    JsonNode orgProfile = getJson("/api/pingan/content-profiles/by-org/4", token).path("data");
    assertThat(orgProfile.path("title").asText()).isEqualTo("Demo Works Company简介");
    assertThat(orgProfile.path("imageAttachment").path("fileKind").asText()).isEqualTo("IMAGE");

    JsonNode inactiveProfile = getJson("/api/pingan/content-profiles/by-org/24", token).path("data");
    assertThat(inactiveProfile.isMissingNode() || inactiveProfile.isNull()).isTrue();

    JsonNode selectedVideos = getJson("/api/pingan/content-profiles/screen-videos?orgId=4", token).path("data");
    assertThat(selectedVideos).hasSize(1);
    assertThat(selectedVideos.get(0).path("videoAttachment").path("originalName").asText()).isEqualTo("宣传片.mp4");

    JsonNode allVideos = getJson("/api/pingan/content-profiles/screen-videos", token).path("data");
    assertThat(allVideos).hasSize(1);
    assertThat(allVideos.get(0).path("videoSortOrder").asInt()).isEqualTo(7);

    mockMvc
        .perform(delete("/api/system/content-profiles/" + activeId + "/video").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    assertThat(getJson("/api/pingan/content-profiles/screen-videos?orgId=4", token).path("data")).isEmpty();

    mockMvc.perform(delete("/api/system/content-profiles/" + inactiveId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  private Map<String, Object> payload(
      Long orgId,
      String title,
      String subtitle,
      String description,
      String videoTitle,
      Integer videoSortOrder,
      String status) {
    return Map.of(
        "orgId", orgId,
        "title", title,
        "subtitle", subtitle,
        "description", description,
        "videoTitle", videoTitle,
        "videoSortOrder", videoSortOrder,
        "status", status);
  }

  private String login(String username, String password) throws Exception {
    JsonNode response = postJson("/api/auth/login", null, Map.of("username", username, "password", password));
    return response.path("data").path("accessToken").asText();
  }

  private JsonNode getJson(String url, String token) throws Exception {
    return objectMapper.readTree(
        mockMvc
            .perform(get(url).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
  }

  private JsonNode postJson(String url, String token, Object body) throws Exception {
    MockHttpServletRequestBuilder request =
        post(url).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    return objectMapper.readTree(
        mockMvc
            .perform(request)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
  }

  private JsonNode putJson(String url, String token, Object body) throws Exception {
    return objectMapper.readTree(
        mockMvc
            .perform(
                put(url)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
  }

  private JsonNode patchJson(String url, String token, Object body) throws Exception {
    return objectMapper.readTree(
        mockMvc
            .perform(
                patch(url)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
  }
}



