package com.pingan.banzu.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingan.banzu.common.BusinessException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class ThreeCheckRecordPayloadCodec {

  private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {};

  private final ObjectMapper objectMapper;

  ThreeCheckRecordPayloadCodec(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  Map<String, Object> read(String payloadJson) {
    if (payloadJson == null || payloadJson.isBlank()) {
      return new LinkedHashMap<>();
    }
    try {
      return objectMapper.readValue(payloadJson, PAYLOAD_TYPE);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("记录扩展字段解析失败：" + exception.getMessage());
    }
  }

  Map<String, Object> readOptional(String payloadJson) {
    return payloadJson == null || payloadJson.isBlank() ? null : read(payloadJson);
  }

  String write(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("记录扩展字段保存失败：" + exception.getMessage());
    }
  }
}
