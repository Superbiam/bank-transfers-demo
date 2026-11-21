package kg.yarrrkady.banktransfersdemo.application.port.idempotency;

import java.security.MessageDigest;
import java.util.UUID;

public record IdempotencyRecord(byte[] requestHash, UUID transactionId) {

    public boolean hasSameHash(byte[] other) {
        return MessageDigest.isEqual(requestHash, other);
    }
}
