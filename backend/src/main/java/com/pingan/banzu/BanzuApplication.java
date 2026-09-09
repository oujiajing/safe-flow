package com.pingan.banzu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan({"com.pingan.banzu.mapper", "com.pingan.banzu.system.mapper"})
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class BanzuApplication {

  public static void main(String[] args) {
    SpringApplication.run(BanzuApplication.class, args);
  }
}
