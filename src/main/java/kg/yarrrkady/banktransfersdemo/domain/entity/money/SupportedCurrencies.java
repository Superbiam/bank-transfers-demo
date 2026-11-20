package kg.yarrrkady.banktransfersdemo.domain.entity.money;

import java.util.Currency;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import kg.yarrrkady.banktransfersdemo.domain.exception.money.InvalidMoneyException;

public final class SupportedCurrencies {
    private static final Map<String, Currency> SUPPORTED_CURRENCIES = 
            Stream.of("USD", "EUR", "KGS", "JPY", "KWD", "GBP")
                .collect(Collectors.toUnmodifiableMap(
                    code -> code,
                    Currency::getInstance
                ));
    
    public static Currency require(String code) {
        Currency currency = code == null ? null : SUPPORTED_CURRENCIES.get(code);
        if (currency == null) {
            throw new InvalidMoneyException("Unsupported currency code: " + code);
        }
        return currency;
    }

    public static void requireSupported(Currency currency) {
        if (currency == null) {
            throw new InvalidMoneyException("Currency must not be null");
        }
        if (!SUPPORTED_CURRENCIES.containsKey(currency.getCurrencyCode())) {
            throw new InvalidMoneyException("Unsupported currency: " + currency.getCurrencyCode());
        }
    }
}
