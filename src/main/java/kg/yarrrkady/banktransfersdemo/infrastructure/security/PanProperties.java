package kg.yarrrkady.banktransfersdemo.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.pan")
public record PanProperties(String encryptionKey, String hmacKey) {}
