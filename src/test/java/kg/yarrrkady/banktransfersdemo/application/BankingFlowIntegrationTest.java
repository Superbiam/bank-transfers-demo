package kg.yarrrkady.banktransfersdemo.application;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import kg.yarrrkady.banktransfersdemo.application.account.AccountService;
import kg.yarrrkady.banktransfersdemo.application.card.CardService;
import kg.yarrrkady.banktransfersdemo.application.card.ChangeCardStatusCommand;
import kg.yarrrkady.banktransfersdemo.application.card.IssueCardCommand;
import kg.yarrrkady.banktransfersdemo.application.transfer.TransferCommand;
import kg.yarrrkady.banktransfersdemo.application.transfer.TransferResult;
import kg.yarrrkady.banktransfersdemo.application.transfer.TransferService;
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

class BankingFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired TransferService transfers;
    @Autowired AccountService accountService;
    @Autowired CardService cardService;

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

}
