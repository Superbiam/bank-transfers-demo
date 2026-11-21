package kg.yarrrkady.banktransfersdemo.infrastructure.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import kg.yarrrkady.banktransfersdemo.application.port.idempotency.IdempotencyRepository;

import java.time.Clock;

@Component
class IdempotencyCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupJob.class);

    private final IdempotencyRepository repository;
    private final Clock clock;

    IdempotencyCleanupJob(IdempotencyRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "PT1H")
    void purge() {
        int removed = repository.deleteExpired(clock.instant());
        log.info("Removed {} expired idempotency keys", removed);
    }
}
