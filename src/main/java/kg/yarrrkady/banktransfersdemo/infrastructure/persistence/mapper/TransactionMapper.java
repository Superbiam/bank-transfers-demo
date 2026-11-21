package kg.yarrrkady.banktransfersdemo.infrastructure.persistence.mapper;

import java.util.Currency;

import kg.yarrrkady.banktransfersdemo.domain.entity.money.Money;
import kg.yarrrkady.banktransfersdemo.domain.entity.transfer.Transaction;
import kg.yarrrkady.banktransfersdemo.infrastructure.persistence.entity.TransactionEntity;

public class TransactionMapper {
    public static Transaction toDomain(TransactionEntity e) {
        return new Transaction(
            e.getId(), e.getFromAccountId(), e.getToAccountId(),
            new Money(e.getDebitAmount(), Currency.getInstance(e.getDebitCurrency())),
            new Money(e.getCreditAmount(), Currency.getInstance(e.getCreditCurrency())),
            e.getRate(), e.getStatus(), e.getIdempotencyKey(), e.getCreatedAt(), e.getComment());
    }
}
