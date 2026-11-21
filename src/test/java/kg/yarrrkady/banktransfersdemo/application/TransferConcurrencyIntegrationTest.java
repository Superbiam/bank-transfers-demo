package kg.yarrrkady.banktransfersdemo.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import kg.yarrrkady.banktransfersdemo.application.transfer.TransferCommand;
import kg.yarrrkady.banktransfersdemo.application.transfer.TransferResult;
import kg.yarrrkady.banktransfersdemo.application.transfer.TransferService;
import kg.yarrrkady.banktransfersdemo.domain.exception.account.InsufficientFundsException;

import static org.assertj.core.api.Assertions.assertThat;

class TransferConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired TransferService transfers;

    @Test
    void shouldNeverOverdrawAccountUnderParallelTransfers() {
        UUID from = openAccount("100.00");
        UUID to = openAccount("0.00");

        List<Callable<TransferResult>> tasks = IntStream.range(0, 20)
            .mapToObj(i -> (Callable<TransferResult>) () ->
                transfers.transfer(command(from, to, "10.00", key())))
            .toList();

        List<Outcome> outcomes = runConcurrently(tasks);

        assertThat(succeeded(outcomes)).hasSize(10);
        assertThat(failures(outcomes))
            .hasSize(10)
            .allSatisfy(e -> assertThat(e).isInstanceOf(InsufficientFundsException.class));
        assertThat(balanceInDb(from)).isEqualByComparingTo("0.00");
        assertThat(balanceInDb(to)).isEqualByComparingTo("100.00");
        assertThat(journalCount(from)).isEqualTo(10);
        assertThat(journalDebitSum(from)).isEqualByComparingTo("100.00");
    }

    @RepeatedTest(5)
    void shouldNotDeadlockOnOppositeTransfers() {
        UUID a = openAccount("1000.00");
        UUID b = openAccount("1000.00");

        List<Callable<TransferResult>> tasks = IntStream.range(0, 40)
            .mapToObj(i -> {
                UUID x = i % 2 == 0 ? a : b;
                UUID y = i % 2 == 0 ? b : a;
                return (Callable<TransferResult>) () ->
                    transfers.transfer(command(x, y, "10.00", key()));
            })
            .toList();

        List<Outcome> outcomes = runConcurrently(tasks);

        assertThat(failures(outcomes)).isEmpty();
        assertThat(balanceInDb(a).add(balanceInDb(b))).isEqualByComparingTo("2000.00");
        assertThat(balanceInDb(a)).isEqualByComparingTo("1000.00"); // по 20 переводов в каждую сторону
        assertThat(journalCount(a) + journalCount(b)).isEqualTo(40);
    }

    @Test
    void shouldCreateSingleTransactionForSameIdempotencyKeyUnderParallelRequests() {
        UUID from = openAccount("100.00");
        UUID to = openAccount("0.00");
        TransferCommand cmd = command(from, to, "10.00", key());

        List<Callable<TransferResult>> tasks = IntStream.range(0, 10)
            .mapToObj(i -> (Callable<TransferResult>) () -> transfers.transfer(cmd))
            .toList();

        List<Outcome> outcomes = runConcurrently(tasks);

        assertThat(failures(outcomes)).isEmpty();
        List<TransferResult> results = succeeded(outcomes);
        assertThat(results.stream().filter(r -> !r.replayed())).hasSize(1);
        assertThat(results.stream().map(r -> r.transaction().id()).distinct()).hasSize(1);
        assertThat(journalCount(from)).isEqualTo(1);
        assertThat(balanceInDb(from)).isEqualByComparingTo("90.00");
        assertThat(balanceInDb(to)).isEqualByComparingTo("10.00");
    }

    // ---------- вспомогательные методы ----------

    private record Outcome(TransferResult result, Throwable error) {
    }

    /** Запускает все задачи одновременно: сначала собирает потоки на старте, потом отпускает защёлку. */
    private List<Outcome> runConcurrently(List<Callable<TransferResult>> tasks) {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Outcome>> futures = tasks.stream()
                .map(task -> pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        return new Outcome(task.call(), null);
                    } catch (Throwable e) {
                        return new Outcome(null, e);
                    }
                }))
                .toList();

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
            return outcomes;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new AssertionError("Concurrent run did not finish in time", e);
        } finally {
            pool.shutdownNow();
        }
    }

    private static List<TransferResult> succeeded(List<Outcome> outcomes) {
        return outcomes.stream().filter(o -> o.error() == null).map(Outcome::result).toList();
    }

    private static List<Throwable> failures(List<Outcome> outcomes) {
        return outcomes.stream().map(Outcome::error).filter(Objects::nonNull).toList();
    }

    private static String key() {
        return "key-" + UUID.randomUUID();
    }

    private BigDecimal balanceInDb(UUID accountId) {
        return jdbc.sql("select balance from accounts where id = :id")
            .param("id", accountId)
            .query(BigDecimal.class)
            .single();
    }

    private BigDecimal journalDebitSum(UUID fromAccountId) {
        return jdbc.sql("select coalesce(sum(debit_amount), 0) from transactions where from_account_id = :id")
            .param("id", fromAccountId)
            .query(BigDecimal.class)
            .single();
    }
}
