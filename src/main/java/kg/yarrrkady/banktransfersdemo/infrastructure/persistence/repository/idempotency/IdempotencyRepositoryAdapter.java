package kg.yarrrkady.banktransfersdemo.infrastructure.persistence.repository.idempotency;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import kg.yarrrkady.banktransfersdemo.application.port.idempotency.IdempotencyRecord;
import kg.yarrrkady.banktransfersdemo.application.port.idempotency.IdempotencyRepository;

@Repository
class IdempotencyRepositoryAdapter implements IdempotencyRepository {

    private final JdbcClient jdbc;

    IdempotencyRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean tryRegister(String userId, String key, byte[] requestHash,
                               Instant now, Instant expiresAt) {
        int rows = jdbc.sql("""
                insert into idempotency_keys (user_id, idempotency_key, request_hash, expires_at)
                values (:userId, :idemKey, :hash, :expiresAt)
                on conflict (user_id, idempotency_key) do update
                   set request_hash = excluded.request_hash,
                       transaction_id = null,
                       expires_at = excluded.expires_at
                 where idempotency_keys.expires_at < :now
                """)
            .param("userId", userId)
            .param("idemKey", key)
            .param("hash", requestHash)
            .param("expiresAt", utc(expiresAt))
            .param("now", utc(now))
            .update();
        return rows == 1;
    }

    @Override
    public Optional<IdempotencyRecord> find(String userId, String key) {
        return jdbc.sql("""
                select request_hash, transaction_id
                  from idempotency_keys
                 where user_id = :userId and idempotency_key = :idemKey
                """)
            .param("userId", userId)
            .param("idemKey", key)
            .query((rs, n) -> new IdempotencyRecord(
                    rs.getBytes("request_hash"),
                    rs.getObject("transaction_id", UUID.class)))
            .optional();
    }

    @Override
    public void attachTransaction(String userId, String key, UUID transactionId) {
        int rows = jdbc.sql("""
                update idempotency_keys set transaction_id = :tx
                 where user_id = :userId and idempotency_key = :idemKey
                """)
            .param("tx", transactionId)
            .param("userId", userId)
            .param("idemKey", key)
            .update();
        if (rows != 1) {
            throw new IllegalStateException("Idempotency key was not registered in this transaction");
        }
    }

    @Override
    public int deleteExpired(Instant now) {
        return jdbc.sql("delete from idempotency_keys where expires_at < :now")
            .param("now", utc(now))
            .update();
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
