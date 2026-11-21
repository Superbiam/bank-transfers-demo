package kg.yarrrkady.banktransfersdemo.application;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import kg.yarrrkady.banktransfersdemo.application.port.account.AccountRepository;
import kg.yarrrkady.banktransfersdemo.application.transfer.TransferCommand;
import kg.yarrrkady.banktransfersdemo.domain.entity.account.Account;
import kg.yarrrkady.banktransfersdemo.domain.entity.account.AccountStatus;
import kg.yarrrkady.banktransfersdemo.domain.entity.money.Money;

@SpringBootTest(properties = {
        "app.pan.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.pan.hmac-key=ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=",
        // потоков в тестах до 40; пул должен быть не меньше, иначе упрёмся в ожидание соединений
        "spring.datasource.hikari.maximum-pool-size=50"
})
abstract class AbstractIntegrationTest {

    // один контейнер на все тестовые классы (singleton-паттерн Testcontainers)
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    static {
        postgres.start();
    }

    @Autowired protected AccountRepository accounts;
    @Autowired protected JdbcClient jdbc;
    @Autowired protected TransactionTemplate tx;

    // сюда: openAccount, createClient, command, balanceOf, journalCount (protected)
    protected UUID openAccount(String balance) {
        UUID clientId = createClient();
        UUID accountId = UUID.randomUUID();
        String number = "ACC" + ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L);
        tx.executeWithoutResult(s -> accounts.insert(Account.restore(
                accountId, clientId, number, Money.of(balance, "USD"), AccountStatus.ACTIVE, 0)));
        return accountId;
    }

    protected UUID createClient() {
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

    protected Money balanceOf(UUID accountId) {
        return accounts.findById(accountId).orElseThrow().getBalance();
    }

    protected int journalCount(UUID fromAccountId) {
        return jdbc.sql("select count(*) from transactions where from_account_id = :id")
            .param("id", fromAccountId)
            .query(Integer.class)
            .single();
    }
    
    protected TransferCommand command(UUID from, UUID to, String amount, String key) {
        return new TransferCommand("operator1", key, from, to, Money.of(amount, "USD"), null);
    }
}