package kg.yarrrkady.banktransfersdemo.application.card;

import java.util.UUID;

import kg.yarrrkady.banktransfersdemo.domain.entity.card.CardStatus;

public record ChangeCardStatusCommand(
    UUID cardId, CardStatus target, String actor, String reason) {}
