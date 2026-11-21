package kg.yarrrkady.banktransfersdemo.application;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import kg.yarrrkady.banktransfersdemo.application.account.AccountService;
import kg.yarrrkady.banktransfersdemo.application.card.CardService;
import kg.yarrrkady.banktransfersdemo.application.card.ChangeCardStatusCommand;
import kg.yarrrkady.banktransfersdemo.application.card.IssueCardCommand;
import kg.yarrrkady.banktransfersdemo.application.port.account.AccountRepository;
import kg.yarrrkady.banktransfersdemo.application.transfer.TransferCommand;
import kg.yarrrkady.banktransfersdemo.application.transfer.TransferResult;
import kg.yarrrkady.banktransfersdemo.application.transfer.TransferService;
import kg.yarrrkady.banktransfersdemo.domain.entity.account.Account;
import kg.yarrrkady.banktransfersdemo.domain.entity.account.AccountStatus;
import kg.yarrrkady.banktransfersdemo.domain.entity.card.Card;
import kg.yarrrkady.banktransfersdemo.domain.entity.card.CardNumber;
import kg.yarrrkady.banktransfersdemo.domain.entity.card.CardStatus;
import kg.yarrrkady.banktransfersdemo.domain.entity.card.CardType;
import kg.yarrrkady.banktransfersdemo.domain.entity.money.Money;
import kg.yarrrkady.banktransfersdemo.domain.exception.account.AccountHasActiveCardsException;
import kg.yarrrkady.banktransfersdemo.domain.exception.account.InsufficientFundsException;
import kg.yarrrkady.banktransfersdemo.domain.exception.idempotency.IdempotencyKeyReusedException;

import static org.assertj.core.api.Assertions.assertThat;
import static kg.yarrrkady.banktransfersdemo.support.DomainAssertions.assertDomainError;

@SpringBootTest(properties = {
        // только для тестов, не секреты
        "app.pan.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.pan.hmac-key=ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA="
})
@Testcontainers
class BankingFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Autowired TransferService transfers;
    @Autowired AccountService accountService;
    @Autowired CardService cardService;
    @Autowired AccountRepository accounts;
    @Autowired JdbcClient jdbc;
    @Autowired TransactionTemplate tx;

    // ---------- перевод и идемпотентность ----------

    @Test
    void transferShouldMoveMoneyAndWriteOneJournalRecord() {
        UUID from = openAccount("100.00");
        UUID to = openAccount("0.00");

        TransferResult result = transfers.transfer(command(from, to, "10.00", "key-1"));

        assertThat(result.replayed()).isFalse();
        assertThat(balanceOf(from)).isEqualTo(Money.of("90.00", "USD"));
        assertThat(balanceOf(to)).isEqualTo(Money.of("10.00", "USD"));
        assertThat(journalCount(from)).isEqualTo(1);
    }

    @Test
    void repeatWithSameKeyAndBodyShouldReturnFirstResultWithoutSecondTransfer() {
        UUID from = openAccount("100.00");
        UUID to = openAccount("0.00");
        TransferCommand cmd = command(from, to, "10.00", "key-2");

        TransferResult first = transfers.transfer(cmd);
        TransferResult second = transfers.transfer(cmd);

        assertThat(second.replayed()).isTrue();
        assertThat(second.transaction().id()).isEqualTo(first.transaction().id());
        assertThat(journalCount(from)).isEqualTo(1);
        assertThat(balanceOf(from)).isEqualTo(Money.of("90.00", "USD"));
        assertThat(balanceOf(to)).isEqualTo(Money.of("10.00", "USD"));
    }

    @Test
    void sameKeyWithDifferentAmountShouldBeRejectedAndChangeNothing() {
        UUID from = openAccount("100.00");
        UUID to = openAccount("0.00");
        transfers.transfer(command(from, to, "10.00", "key-3"));

        assertDomainError(
            () -> transfers.transfer(command(from, to, "20.00", "key-3")),
            IdempotencyKeyReusedException.class, IdempotencyKeyReusedException.CODE);

        assertThat(balanceOf(from)).isEqualTo(Money.of("90.00", "USD"));
        assertThat(journalCount(from)).isEqualTo(1);
    }

    @Test
    void failedTransferShouldNotBurnIdempotencyKey() {
        UUID from = openAccount("100.00");
        UUID to = openAccount("0.00");

        assertDomainError(
            () -> transfers.transfer(command(from, to, "500.00", "key-4")),
            InsufficientFundsException.class, InsufficientFundsException.CODE);

        // ключ откатился вместе с транзакцией: тот же ключ с другой суммой выполняется заново
        TransferResult retry = transfers.transfer(command(from, to, "10.00", "key-4"));

        assertThat(retry.replayed()).isFalse();
        assertThat(balanceOf(from)).isEqualTo(Money.of("90.00", "USD"));
    }

    // ---------- закрытие счёта ----------

    @Test
    void closeShouldBeRejectedWhileCardIsActiveAndAllowedAfterBlocking() {
        UUID accountId = openAccount("0.00");
        Card card = cardService.issue(new IssueCardCommand(
                accountId, new CardNumber("4111111111111111"), CardType.DEBIT,
                YearMonth.now(ZoneOffset.UTC).plusYears(3)));

        assertDomainError(
            () -> accountService.close(accountId),
            AccountHasActiveCardsException.class, AccountHasActiveCardsException.CODE);
        assertThat(accounts.findById(accountId).orElseThrow().getStatus())
            .isEqualTo(AccountStatus.ACTIVE);

        cardService.changeStatus(new ChangeCardStatusCommand(
                card.getId(), CardStatus.BLOCKED, "operator1", "lost card"));
        accountService.close(accountId);

        assertThat(accounts.findById(accountId).orElseThrow().getStatus())
            .isEqualTo(AccountStatus.CLOSED);
    }

    // ---------- вспомогательные методы ----------

    private TransferCommand command(UUID from, UUID to, String amount, String key) {
        return new TransferCommand("operator1", key, from, to, Money.of(amount, "USD"), null);
    }

    private UUID openAccount(String balance) {
        UUID clientId = createClient();
        UUID accountId = UUID.randomUUID();
        String number = "ACC" + ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L);
        tx.executeWithoutResult(s -> accounts.insert(Account.restore(
                accountId, clientId, number, Money.of(balance, "USD"), AccountStatus.ACTIVE, 0)));
        return accountId;
    }

    private UUID createClient() {
        UUID id = UUID.randomUUID();
        // колонки clients угаданы по ТЗ: сверьте с \d clients
        jdbc.sql("""
                insert into clients (id, last_name, first_name, middle_name, email, birth_date)
                values (:id, 'Test', 'Test', 'Test', :email, date '1990-01-01')
                """)
            .param("id", id)
            .param("email", id + "@test.local")
            .update();
        return id;
    }

    private Money balanceOf(UUID accountId) {
        return accounts.findById(accountId).orElseThrow().getBalance();
    }

    private int journalCount(UUID fromAccountId) {
        return jdbc.sql("select count(*) from transactions where from_account_id = :id")
            .param("id", fromAccountId)
            .query(Integer.class)
            .single();
    }
}
