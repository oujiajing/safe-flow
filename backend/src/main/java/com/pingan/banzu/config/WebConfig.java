package com.pingan.banzu.config;

import com.pingan.banzu.security.AuthInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final AuthInterceptor authInterceptor;

  public WebConfig(AuthInterceptor authInterceptor) {
    this.authInterceptor = authInterceptor;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedMethods("*")
        .allowedOriginPatterns("*")
        .exposedHeaders(RequestCorrelationFilter.HEADER_NAME)
        .allowCredentials(true);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(authInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/attachments/**",
            "/api/assets/**");
  }

  @Bean
  MappedInterceptor prometheusAuthInterceptor() {
    return new MappedInterceptor(new String[] {"/actuator/prometheus"}, authInterceptor);
  }

}
