package kg.yarrrkady.banktransfersdemo.application.card;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import kg.yarrrkady.banktransfersdemo.application.port.account.AccountRepository;
import kg.yarrrkady.banktransfersdemo.application.port.card.CardRepository;
import kg.yarrrkady.banktransfersdemo.application.port.card.CardStatusHistoryRepository;
import kg.yarrrkady.banktransfersdemo.domain.entity.account.Account;
import kg.yarrrkady.banktransfersdemo.domain.entity.card.Card;
import kg.yarrrkady.banktransfersdemo.domain.entity.card.CardStatusChange;

@Service
public class CardService {

    private final AccountRepository accounts;
    private final CardRepository cards;
    private final CardStatusHistoryRepository history;
    private final Clock clock;

    public CardService(AccountRepository accounts, CardRepository cards, CardStatusHistoryRepository history,
            Clock clock) {
        this.accounts = accounts;
        this.cards = cards;
        this.history = history;
        this.clock = clock;
    }

    @Transactional
    public Card issue(IssueCardCommand cmd) {
        Account account = accounts.lockAllByIds(Set.of(cmd.accountId())).get(0);

        Card card = Card.issue(UUID.randomUUID(), account, cmd.number(),
                cmd.type(), cmd.expiry(), clock.instant());

        cards.insert(card, cmd.number());
        return card;
    }

    @Transactional
    public Card changeStatus(ChangeCardStatusCommand cmd) {
        Card card = cards.lockById(cmd.cardId());

        CardStatusChange change = card.changeStatus(
                cmd.target(), cmd.actor(), cmd.reason(), clock.instant());

        cards.save(card);
        history.insert(change);
        return card;
    }
}
