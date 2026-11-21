package kg.yarrrkady.banktransfersdemo.infrastructure.persistence.mapper;

import java.util.Currency;

import kg.yarrrkady.banktransfersdemo.domain.entity.account.Account;
import kg.yarrrkady.banktransfersdemo.domain.entity.money.Money;
import kg.yarrrkady.banktransfersdemo.infrastructure.persistence.entity.AccountEntity;

public class AccountMapper {
    
    public static Account toDomain(AccountEntity e) {
        return Account.restore(
            e.getId(), e.getClientId(), e.getAccountNumber(),
            new Money(e.getBalance(), Currency.getInstance(e.getCurrency())),
            e.getStatus(), e.getVersion());
    }
}
