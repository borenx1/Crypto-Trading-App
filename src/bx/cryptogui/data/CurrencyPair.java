package bx.cryptogui.data;

public class CurrencyPair {

    public static final CurrencyPair BTC_USD = new CurrencyPair(Currency.BTC, Currency.USD);
    public static final CurrencyPair BTC_NZD = new CurrencyPair(Currency.BTC, Currency.NZD);
    public static final CurrencyPair LTC_USD = new CurrencyPair(Currency.LTC, Currency.USD);
    public static final CurrencyPair LTC_NZD = new CurrencyPair(Currency.LTC, Currency.NZD);
    public static final CurrencyPair ETH_USD = new CurrencyPair(Currency.ETH, Currency.USD);
    public static final CurrencyPair ETH_NZD = new CurrencyPair(Currency.ETH, Currency.NZD);
    public static final CurrencyPair USD_NZD = new CurrencyPair(Currency.USD, Currency.NZD);

    private final Currency baseCurrency;
    private final Currency quoteCurrency;

    public CurrencyPair(Currency baseCurrency, Currency quoteCurrency) throws IllegalArgumentException {
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        if (baseCurrency == quoteCurrency) {
            throw new IllegalArgumentException("Currencies must be different");
        }
    }

    public Currency getBaseCurrency() {
        return baseCurrency;
    }

    public Currency getQuoteCurrency() {
        return quoteCurrency;
    }

    @Override
    public String toString() {
        return baseCurrency + "-" + quoteCurrency;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CurrencyPair &&
                baseCurrency == ((CurrencyPair) obj).baseCurrency && quoteCurrency == ((CurrencyPair) obj).quoteCurrency;
    }

    @Override
    public int hashCode() {
        return baseCurrency.hashCode() + quoteCurrency.hashCode()*11;
    }
}