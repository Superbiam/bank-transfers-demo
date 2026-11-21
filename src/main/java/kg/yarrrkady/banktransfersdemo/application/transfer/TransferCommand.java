package kg.yarrrkady.banktransfersdemo.application.transfer;

import java.util.UUID;

import kg.yarrrkady.banktransfersdemo.domain.entity.money.Money;

public record TransferCommand(
        String userId, String idempotencyKey, UUID fromAccountId, UUID toAccountId,
        Money amount, String comment) {}
