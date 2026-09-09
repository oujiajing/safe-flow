package com.pingan.banzu.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public String hash(String rawPassword) {
    return "{bcrypt}" + encoder.encode(rawPassword);
  }

  public boolean matches(String rawPassword, String storedHash) {
    if (storedHash == null) {
      return false;
    }
    if (storedHash.startsWith("{noop}")) {
      return storedHash.substring("{noop}".length()).equals(rawPassword);
    }
    if (storedHash.startsWith("{bcrypt}")) {
      return encoder.matches(rawPassword, storedHash.substring("{bcrypt}".length()));
    }
    return encoder.matches(rawPassword, storedHash);
  }
}
