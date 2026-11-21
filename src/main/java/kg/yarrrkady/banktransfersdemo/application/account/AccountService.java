package kg.yarrrkady.banktransfersdemo.application.account;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;


import jakarta.transaction.Transactional;
import kg.yarrrkady.banktransfersdemo.application.port.account.AccountRepository;
import kg.yarrrkady.banktransfersdemo.application.port.card.CardRepository;
import kg.yarrrkady.banktransfersdemo.domain.entity.account.Account;

@Service
public class AccountService {

    private final AccountRepository accounts;
    private final CardRepository cards;
    private final Clock clock;

    public AccountService(AccountRepository accounts, CardRepository cards, Clock clock) {
        this.accounts = accounts;
        this.cards = cards;
        this.clock = clock;
    }

    @Transactional
    public void close(UUID accountId) {
        Instant now = clock.instant();
        Account account = lock(accountId);

        boolean hasActiveCards = cards.existsActiveByAccountId(
                accountId, LocalDate.ofInstant(now, ZoneOffset.UTC));

        account.close(hasActiveCards);
        accounts.save(account);
    }

    private Account lock(UUID accountId) {
        return accounts.lockAllByIds(Set.of(accountId)).get(0);
    }
}
