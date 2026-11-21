package kg.yarrrkady.banktransfersdemo.application.card;

import java.time.YearMonth;
import java.util.UUID;

import kg.yarrrkady.banktransfersdemo.domain.entity.card.CardNumber;
import kg.yarrrkady.banktransfersdemo.domain.entity.card.CardType;

public record IssueCardCommand(
    UUID accountId, CardNumber number, CardType type, YearMonth expiry) {}

