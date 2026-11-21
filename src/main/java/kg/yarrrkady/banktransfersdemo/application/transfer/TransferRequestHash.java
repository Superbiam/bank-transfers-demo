package kg.yarrrkady.banktransfersdemo.application.transfer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import kg.yarrrkady.banktransfersdemo.domain.entity.money.Money;

public final class TransferRequestHash {

    private TransferRequestHash() {
    }

    public static byte[] of(UUID fromAccountId, UUID toAccountId, Money amount, String comment) {
        String canonical = fromAccountId + "|" + toAccountId + "|"
                + amount.amount().toPlainString() + "|"
                + amount.currency().getCurrencyCode() + "|"
                + (comment == null ? "" : comment);
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
