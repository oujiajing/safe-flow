package com.pingan.banzu.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@ConditionalOnProperty(
    name = "pingan.storage.provider",
    havingValue = "local",
    matchIfMissing = true)
public class LocalAssetController {

  private static final String ASSET_PREFIX = "/api/assets/";
  private static final String MINI_PROGRAM_PREFIX = "mini-program/";

  private final ResourceLoader resourceLoader;

  public LocalAssetController(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  @GetMapping("/api/assets/**")
  public ResponseEntity<Resource> downloadAsset(HttpServletRequest request) {
    String objectKey = request.getRequestURI().substring(ASSET_PREFIX.length());
    String safeObjectKey = requireObjectKey(objectKey);
    Resource resource = findAsset(safeObjectKey);
    MediaType mediaType =
        MediaTypeFactory.getMediaType(safeObjectKey).orElse(MediaType.APPLICATION_OCTET_STREAM);
    return ResponseEntity.ok().contentType(mediaType).body(resource);
  }

  private Resource findAsset(String objectKey) {
    for (Resource resource : assetResources(objectKey)) {
      if (resource.exists() && resource.isReadable()) {
        return resource;
      }
    }
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "资源不存在");
  }

  private List<Resource> assetResources(String objectKey) {
    List<Resource> resources = new ArrayList<>();
    resources.add(resourceLoader.getResource("classpath:/assets/" + objectKey));
    if (objectKey.startsWith(MINI_PROGRAM_PREFIX)) {
      String miniProgramAssetPath = objectKey.substring(MINI_PROGRAM_PREFIX.length());
      resources.add(resourceLoader.getResource("classpath:/assets/" + miniProgramAssetPath));
      resources.add(resourceLoader.getResource("file:mini-program/assets/" + miniProgramAssetPath));
      resources.add(resourceLoader.getResource("file:../mini-program/assets/" + miniProgramAssetPath));
    }
    return resources;
  }

  private String requireObjectKey(String objectKey) {
    if (objectKey == null
        || objectKey.isBlank()
        || objectKey.startsWith("/")
        || objectKey.contains("..")) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "资源不存在");
    }
    return objectKey;
  }
}
