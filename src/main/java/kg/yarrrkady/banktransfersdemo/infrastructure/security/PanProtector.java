package kg.yarrrkady.banktransfersdemo.infrastructure.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class PanProtector {

    private static final byte KEY_VERSION = 1;
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_LENGTH = 32;

    private final SecretKeySpec encryptionKey;
    private final SecretKeySpec hmacKey;
    private final SecureRandom random = new SecureRandom();

    PanProtector(PanProperties properties) {
        this.encryptionKey = new SecretKeySpec(
                decodeKey(properties.encryptionKey(), "app.pan.encryption-key"), "AES");
        this.hmacKey = new SecretKeySpec(
                decodeKey(properties.hmacKey(), "app.pan.hmac-key"), "HmacSHA256");
    }

    public byte[] encrypt(UUID cardId, String pan) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            random.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(aad(cardId));
            byte[] encrypted = cipher.doFinal(pan.getBytes(StandardCharsets.US_ASCII));

            return ByteBuffer.allocate(1 + NONCE_LENGTH + encrypted.length)
                    .put(KEY_VERSION)
                    .put(nonce)
                    .put(encrypted)
                    .array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PAN encryption failed", e);
        }
    }

    public byte[] hmac(String pan) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            return mac.doFinal(pan.getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PAN hashing failed", e);
        }
    }

    private static byte[] aad(UUID cardId) {
        return ByteBuffer.allocate(16)
                .putLong(cardId.getMostSignificantBits())
                .putLong(cardId.getLeastSignificantBits())
                .array();
    }

    private static byte[] decodeKey(String base64, String name) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalStateException(name + " is not set");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(name + " must be valid base64");
        }
        if (key.length != KEY_LENGTH) {
            throw new IllegalStateException(name + " must decode to " + KEY_LENGTH + " bytes");
        }
        return key;
    }
}
