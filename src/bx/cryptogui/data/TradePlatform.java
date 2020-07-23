package bx.cryptogui.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TradePlatform {

    public static final TradePlatform WEX_BTC_USD = new TradePlatform(Exchange.WEX, CurrencyPair.BTC_USD);
    public static final TradePlatform BITSTAMP_BTC_USD = new TradePlatform(Exchange.BITSTAMP, CurrencyPair.BTC_USD);
    public static final TradePlatform COINBASE_BTC_USD = new TradePlatform(Exchange.COINBASE, CurrencyPair.BTC_USD);
    public static final TradePlatform CRYPTOPIA_BTC_USD = new TradePlatform(Exchange.CRYPTOPIA, CurrencyPair.BTC_USD);
    public static final TradePlatform CRYPTOPIA_BTC_NZD = new TradePlatform(Exchange.CRYPTOPIA, CurrencyPair.BTC_NZD);
    public static final TradePlatform KIWICOIN_BTC_NZD = new TradePlatform(Exchange.KIWICOIN, CurrencyPair.BTC_NZD);
    public static final Map<TradePlatform, String> TYPE_2_TABLE;    // TODO remove this
    static {
        HashMap<TradePlatform, String> tempMap = new HashMap<>();
        tempMap.put(WEX_BTC_USD, "wex_btc_usd");
        tempMap.put(BITSTAMP_BTC_USD, "bitstamp_btc_usd");
        tempMap.put(COINBASE_BTC_USD, "coinbase_btc_usd");
        TYPE_2_TABLE = Collections.unmodifiableMap(tempMap);
    }

    private final Exchange exchange;
    private final CurrencyPair currencyPair;

    public TradePlatform(Exchange exchange, CurrencyPair currencyPair) {
        this.exchange = exchange;
        this.currencyPair = currencyPair;
    }

    public Exchange getExchange() {
        return exchange;
    }

    public CurrencyPair getCurrencyPair() {
        return currencyPair;
    }

    public Currency getCurrency1() {
        return currencyPair.getBaseCurrency();
    }

    public Currency getCurrency2() {
        return currencyPair.getQuoteCurrency();
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", exchange, currencyPair.toString());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TradePlatform &&
                exchange == ((TradePlatform) obj).exchange && currencyPair.equals(((TradePlatform) obj).currencyPair);
    }

    @Override
    public int hashCode() {
        return exchange.hashCode()^3 + currencyPair.hashCode();
    }
}
