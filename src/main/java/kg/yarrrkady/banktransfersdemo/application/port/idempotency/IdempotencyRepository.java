package kg.yarrrkady.banktransfersdemo.application.port.idempotency;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository {
    boolean tryRegister(String userId, String key, byte[] requestHash, Instant now, Instant expiresAt);
    Optional<IdempotencyRecord> find(String userId, String key);
    void attachTransaction(String userId, String key, UUID transactionId);
    int deleteExpired(Instant now);
}
