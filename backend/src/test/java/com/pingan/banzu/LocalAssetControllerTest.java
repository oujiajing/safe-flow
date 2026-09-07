package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pingan.banzu.controller.LocalAssetController;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class LocalAssetControllerTest {

  private final LocalAssetController controller =
      new LocalAssetController(new DefaultResourceLoader());

  @Test
  void servesMiniProgramAssetsWhenLocalStorageIsActive() throws Exception {
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/assets/mini-program/icons/fallback.txt");

    ResponseEntity<?> response = controller.downloadAsset(request);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(response.getBody()).isNotNull();
  }

  @Test
  void servesBundledMiniProgramBannerWithoutPackagingItInTheClient() throws Exception {
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/assets/mini-program/banners/1.jpg");

    ResponseEntity<?> response = controller.downloadAsset(request);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
    assertThat(response.getBody()).isNotNull();
  }

  @Test
  void rejectsMissingOrUnsafeAssets() {
    MockHttpServletRequest missing =
        new MockHttpServletRequest("GET", "/api/assets/mini-program/icons/missing.png");
    MockHttpServletRequest unsafe =
        new MockHttpServletRequest("GET", "/api/assets/mini-program/../application.yml");

    assertThatThrownBy(() -> controller.downloadAsset(missing))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("404 NOT_FOUND");
    assertThatThrownBy(() -> controller.downloadAsset(unsafe))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("404 NOT_FOUND");
  }
}
