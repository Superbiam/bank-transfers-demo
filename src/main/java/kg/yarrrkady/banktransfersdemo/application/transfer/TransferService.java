package kg.yarrrkady.banktransfersdemo.application.transfer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import kg.yarrrkady.banktransfersdemo.application.port.account.AccountRepository;
import kg.yarrrkady.banktransfersdemo.application.port.idempotency.IdempotencyRecord;
import kg.yarrrkady.banktransfersdemo.application.port.idempotency.IdempotencyRepository;
import kg.yarrrkady.banktransfersdemo.application.port.money.RateProvider;
import kg.yarrrkady.banktransfersdemo.application.port.transfer.TransactionRepository;
import kg.yarrrkady.banktransfersdemo.domain.entity.account.Account;
import kg.yarrrkady.banktransfersdemo.domain.entity.money.ExchangeRate;
import kg.yarrrkady.banktransfersdemo.domain.entity.transfer.Transaction;
import kg.yarrrkady.banktransfersdemo.domain.entity.transfer.TransferDomainService;
import kg.yarrrkady.banktransfersdemo.domain.exception.idempotency.IdempotencyKeyReusedException;
import kg.yarrrkady.banktransfersdemo.domain.exception.transfer.SameAccountTransferException;

@Service
public class TransferService {

    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final IdempotencyRepository idempotency;
    private final RateProvider rates;
    private final TransferDomainService domain;
    private final Clock clock;

    public TransferService(AccountRepository accounts, TransactionRepository transactions,
            IdempotencyRepository idempotency, RateProvider rates, TransferDomainService domain, Clock clock) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.idempotency = idempotency;
        this.rates = rates;
        this.domain = domain;
        this.clock = clock;
    }

    @Transactional
    public TransferResult transfer(TransferCommand cmd) {
        Instant now = clock.instant();
        byte[] hash = TransferRequestHash.of(
                cmd.fromAccountId(), cmd.toAccountId(), cmd.amount(), cmd.comment());

        if (!idempotency.tryRegister(cmd.userId(), cmd.idempotencyKey(),
                hash, now, now.plus(IDEMPOTENCY_TTL))) {
            return replay(cmd, hash);
        }

        if (cmd.fromAccountId().equals(cmd.toAccountId())) {
            throw new SameAccountTransferException("Cannot transfer to the same account");
        }

        Map<UUID, Account> locked = accounts
                .lockAllByIds(Set.of(cmd.fromAccountId(), cmd.toAccountId()))
                .stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));
        Account from = locked.get(cmd.fromAccountId());
        Account to = locked.get(cmd.toAccountId());

        ExchangeRate rate = from.getCurrency().equals(to.getCurrency())
                ? null
                : rates.rate(from.getCurrency(), to.getCurrency());

        Transaction tx = domain.execute(UUID.randomUUID(), from, to, cmd.amount(), rate,
                cmd.idempotencyKey(), cmd.comment(), now);

        accounts.save(from);
        accounts.save(to);
        transactions.insert(tx);
        idempotency.attachTransaction(cmd.userId(), cmd.idempotencyKey(), tx.id());
        return new TransferResult(tx, false);
    }

    private TransferResult replay(TransferCommand cmd, byte[] hash) {
        IdempotencyRecord existing = idempotency.find(cmd.userId(), cmd.idempotencyKey())
                .orElseThrow();
        if (!existing.hasSameHash(hash)) {
            throw new IdempotencyKeyReusedException("Idempotency key was used with another request");
        }
        Transaction tx = transactions.findById(existing.transactionId()).orElseThrow();
        return new TransferResult(tx, true);
    }
}
