package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pingan.banzu.config.JwtProperties;
import com.pingan.banzu.config.ProductionConfigurationValidator;
import com.pingan.banzu.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

class ProductionConfigurationValidatorTest {

  @Test
  void rejectsPublishedDevelopmentCredentialsWithoutLoggingTheirValues() {
    ProductionConfigurationValidator validator =
        validator(
            "jdbc:postgresql://localhost:5432/pingan_banzu_verify",
            "pingan",
            "pingan",
            "pingan-banzu-local-dev-secret-at-least-32-bytes",
            "minioadmin",
            "minioadmin");

    assertThatThrownBy(validator::afterPropertiesSet)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(
            "PINGAN_JWT_SECRET",
            "PINGAN_DB_PASSWORD",
            "PINGAN_MINIO_ACCESS_KEY",
            "PINGAN_MINIO_SECRET_KEY")
        .hasMessageNotContaining("pingan-banzu-local-dev-secret")
        .hasMessageNotContaining("minioadmin");
  }

  @Test
  void acceptsExplicitProductionSecrets() {
    ProductionConfigurationValidator validator =
        validator(
            "jdbc:postgresql://db.internal:5432/pingan_banzu",
            "pingan_app",
            "database-secret-from-vault",
            "jwt-signing-secret-from-vault-at-least-32-bytes",
            "production-access-key",
            "production-secret-key");

    assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
  }

  @Test
  void localStorageDoesNotRequireUnusedMinioCredentials() {
    DataSourceProperties dataSource = dataSource("jdbc:postgresql://db/pingan", "app", "db-secret");
    StorageProperties local =
        new StorageProperties("local", "/srv/pingan/uploads", "", "", "", "", "", "", 15);
    ProductionConfigurationValidator validator =
        new ProductionConfigurationValidator(
            dataSource,
            new JwtProperties("pingan-banzu", "jwt-secret-from-vault-at-least-32-bytes", 60),
            local);

    assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
  }

  private ProductionConfigurationValidator validator(
      String url,
      String username,
      String password,
      String jwtSecret,
      String minioAccessKey,
      String minioSecretKey) {
    StorageProperties storage =
        new StorageProperties(
            "minio",
            "./uploads",
            "https://minio.internal",
            "https://files.example.com",
            "pingan-production",
            minioAccessKey,
            minioSecretKey,
            "us-east-1",
            15);
    return new ProductionConfigurationValidator(
        dataSource(url, username, password),
        new JwtProperties("pingan-banzu", jwtSecret, 60),
        storage);
  }

  private DataSourceProperties dataSource(String url, String username, String password) {
    DataSourceProperties properties = new DataSourceProperties();
    properties.setUrl(url);
    properties.setUsername(username);
    properties.setPassword(password);
    return properties;
  }
}
