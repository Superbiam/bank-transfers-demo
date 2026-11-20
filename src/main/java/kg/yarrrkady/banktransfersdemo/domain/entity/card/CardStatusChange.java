package kg.yarrrkady.banktransfersdemo.domain.entity.card;

import java.time.Instant;
import java.util.UUID;

public record CardStatusChange(UUID cardId, CardStatus from, CardStatus to,
                               String actor, String reason, Instant changedAt) {}
