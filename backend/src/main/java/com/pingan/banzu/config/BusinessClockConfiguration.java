package com.pingan.banzu.config;

import com.pingan.banzu.service.ThreeCheckOverdueSupport;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class BusinessClockConfiguration {

  @Bean
  public Clock businessClock(
      @Value("${pingan.business-clock.fixed-instant:}") String fixedInstant) {
    if (StringUtils.hasText(fixedInstant)) {
      return Clock.fixed(Instant.parse(fixedInstant), ThreeCheckOverdueSupport.BUSINESS_ZONE);
    }
    return Clock.system(ThreeCheckOverdueSupport.BUSINESS_ZONE);
  }
}
