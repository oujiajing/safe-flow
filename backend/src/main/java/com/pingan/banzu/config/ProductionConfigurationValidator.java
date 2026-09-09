package com.pingan.banzu.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionConfigurationValidator implements InitializingBean {

  static final String DEFAULT_JWT_SECRET = "YOUR_JWT_SECRET_HERE";

  private final DataSourceProperties dataSource;
  private final JwtProperties jwt;
  private final StorageProperties storage;

  public ProductionConfigurationValidator(
      DataSourceProperties dataSource, JwtProperties jwt, StorageProperties storage) {
    this.dataSource = dataSource;
    this.jwt = jwt;
    this.storage = storage;
  }

  @Override
  public void afterPropertiesSet() {
    List<String> invalid = new ArrayList<>();
    requireSecret("PINGAN_JWT_SECRET", jwt.secret(), DEFAULT_JWT_SECRET, invalid);
    requireText("PINGAN_DB_URL", dataSource.getUrl(), invalid);
    requireText("PINGAN_DB_USERNAME", dataSource.getUsername(), invalid);
    requireSecret("PINGAN_DB_PASSWORD", dataSource.getPassword(), "YOUR_DB_PASSWORD_HERE", invalid);

    if (storage.isMinio()) {
      requireText("PINGAN_MINIO_ENDPOINT", storage.endpoint(), invalid);
      requireText("PINGAN_MINIO_BUCKET", storage.bucket(), invalid);
      requireSecret("PINGAN_MINIO_ACCESS_KEY", storage.accessKey(), "YOUR_MINIO_ACCESS_KEY_HERE", invalid);
      requireSecret("PINGAN_MINIO_SECRET_KEY", storage.secretKey(), "YOUR_MINIO_SECRET_KEY_HERE", invalid);
    }
    if (!storage.isLocal() && !storage.isMinio()) {
      invalid.add("PINGAN_STORAGE_PROVIDER");
    }
    if (!invalid.isEmpty()) {
      throw new IllegalStateException(
          "生产配置缺失或仍使用公开默认值，请通过受控 Secret 注入: " + String.join(", ", invalid));
    }
  }

  private static void requireText(String name, String value, List<String> invalid) {
    if (value == null || value.isBlank()) {
      invalid.add(name);
    }
  }

  private static void requireSecret(
      String name, String value, String insecureDefault, List<String> invalid) {
    if (value == null || value.isBlank() || insecureDefault.equals(value)) {
      invalid.add(name);
    }
  }
}


