package kg.yarrrkady.banktransfersdemo.infrastructure.rates;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;

import org.springframework.stereotype.Component;

import kg.yarrrkady.banktransfersdemo.application.port.money.RateProvider;
import kg.yarrrkady.banktransfersdemo.domain.entity.money.ExchangeRate;
import kg.yarrrkady.banktransfersdemo.domain.exception.money.RateUnavailableException;

@Component
class FixedRateProvider implements RateProvider {

    private static final Map<String, String> RATES = Map.of(
            "USD/EUR", "0.92000000",
            "EUR/USD", "1.08700000",
            "USD/KGS", "87.50000000",
            "KGS/USD", "0.01140000",
            "USD/JPY", "148.50000000",
            "JPY/USD", "0.00673400",
            "USD/GBP", "0.76000000",
            "GBP/USD", "1.31578947",
            "EUR/GBP", "0.82608696",
            "GBP/EUR", "1.21052632");

    @Override
    public ExchangeRate rate(Currency from, Currency to) {
        String value = RATES.get(from.getCurrencyCode() + "/" + to.getCurrencyCode());
        if (value == null) {
            throw new RateUnavailableException(
                    "No rate for %s/%s".formatted(from.getCurrencyCode(), to.getCurrencyCode()));
        }
        return new ExchangeRate(from, to, new BigDecimal(value));
    }
}
