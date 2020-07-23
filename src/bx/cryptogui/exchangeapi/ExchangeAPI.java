package bx.cryptogui.exchangeapi;

import bx.cryptogui.data.*;
import bx.cryptogui.data.Currency;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Function;

public abstract class ExchangeAPI {

    private static Long nonce;

    private Authorisation authorisation;
    private final Map<CurrencyPair, PairInfo> pairInfoMap = new HashMap<>();

    private long lastNonce = 0;

    public ExchangeAPI() {}

    public ExchangeAPI(Authorisation authorisation) {
        setAuthorisation(authorisation);
    }

    public ExchangeAPI(String key, String secret) {
        this(new Authorisation(key, secret));
    }

    public abstract Exchange getExchange();
    public abstract String convertCurrencyPair(CurrencyPair currencyPair);
//    public abstract PairInfo getPairInfo(CurrencyPair currencyPair);

    /**
     * Public API method.
     * @param currencyPair Currency pair
     * @return Ticker
     * @throws Exception Exception
     */
    public abstract Ticker getTicker(CurrencyPair currencyPair) throws Exception;
    /**
     * Public API method. Bids (buys) and asks (sells) order books.
     * @param currencyPair currency pair
     * @return array, first is bids, second is asks
     * @throws Exception Exception
     */
    public abstract List<Order>[] getOrderBook(CurrencyPair currencyPair) throws Exception;
    /**
     * Public API method. Order descending by time.
     * @param currencyPair currency pair
     * @return List of trades, or null if no trades for that currency pair.
     * @throws Exception Exception
     */
    public abstract List<Transaction> getTrades(CurrencyPair currencyPair) throws Exception;
    /**
     * Public API method. Order descending by time.
     * @return Map of all supported platforms (currency pairs) mapped to a list of trades
     * @throws Exception Exception
     */
    public abstract Map<TradePlatform, List<Transaction>> getAllTrades() throws Exception;
    /**
     * Private API method.
     * @return account balance
     * @throws Exception Exception
     */
    public abstract Map<Currency, Double> getBalance() throws Exception;
    /**
     * Private API method.
     * @return account open orders
     * @throws Exception Exception
     */
    public abstract List<Order> getOpenOrders() throws Exception;
    /**
     * Private API method.
     * @return account trade history
     * @throws Exception Exception
     */
    public abstract List<Transaction> getTradeHistory() throws Exception;

    public final Authorisation getAuthorisation() {
        return authorisation;
    }

    public final String getKey() {
        return isAuthorised() ? authorisation.getKey() : null;
    }

    public final String getSecret() {
        return isAuthorised() ? authorisation.getSecret() : null;
    }

    public final void setAuthorisation(Authorisation authorisation) {
        this.authorisation = authorisation;
    }

    public final boolean isAuthorised() {
        return authorisation != null;
    }

    public String getSignature(byte[] data) {   // TODO abstract this
        try {
            Mac sha512 = Mac.getInstance("HMACSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(authorisation.getSecret().getBytes("UTF-8"), "HmacSHA512");
            sha512.init(secretKey);
            return new String(Hex.encodeHex(sha512.doFinal(data)));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] urlEncode(Map<String, String> parameters) {
        List<String> params = new ArrayList<>();
        try {
            for (Map.Entry<String, String> entry: parameters.entrySet()) {
                String keyWord = URLEncoder.encode(entry.getKey(), "UTF-8");
                String arg = URLEncoder.encode(entry.getValue(), "UTF-8");
                params.add(keyWord + "=" + arg);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            return String.join("&", params).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.out);
            return new byte[] {};
        }
    }

    public static HTTPResponse getRequest(URL url, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        // Headers
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            StringBuilder s = new StringBuilder();
            reader.lines().forEachOrdered(s::append);
            return new HTTPResponse(connection.getHeaderFields(), s.toString(), connection.getResponseCode(), connection.getResponseMessage());
        }
    }

    public static HTTPResponse getRequest(URL url) throws IOException {
        return getRequest(url, new HashMap<>());
    }

    public static HTTPResponse postRequest(URL url, Map<String, String> headers, Map<String, String> parameters) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        // headers
        connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
        for (Map.Entry<String, String> entry: headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
        // parameters
        if (parameters != null && !parameters.isEmpty()) {
            connection.setDoOutput(true);
            connection.getOutputStream().write(urlEncode(parameters));
        }
        // get response
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            StringBuilder s = new StringBuilder();
            reader.lines().forEachOrdered(s::append);
            return new HTTPResponse(connection.getHeaderFields(), s.toString(), connection.getResponseCode(), connection.getResponseMessage());
        }
    }


    public static long getNonce() {
        if (nonce == null) {
            nonce = System.currentTimeMillis() / 1000;
        }
        return ++nonce;
    }


    public static class PairInfo {

        private final CurrencyPair pair;
        private final double minBase;
        private final double maxBase;
        private final int baseDecimals;
        private final double minPrice;
        private final double maxPrice;
        private final int priceDecimals;

        public PairInfo(CurrencyPair pair, double minBase, double maxBase, int baseDecimals,
                        double minPrice, double maxPrice, int priceDecimals) {
            this.pair = pair;
            if (pair == null) {
                throw new NullPointerException();
            }
            this.minBase = minBase;
            this.maxBase = maxBase;
            this.baseDecimals = baseDecimals;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
            this.priceDecimals = priceDecimals;
        }

        public CurrencyPair getPair() {
            return pair;
        }

        public double getMinBase() {
            return minBase;
        }

        public double getMaxBase() {
            return maxBase;
        }

        public int getBaseDecimals() {
            return baseDecimals;
        }

        public double getMinPrice() {
            return minPrice;
        }

        public double getMaxPrice() {
            return maxPrice;
        }

        public int getPriceDecimals() {
            return priceDecimals;
        }

        public PairInfo withMinBase(double minBase) {
            return new PairInfo(pair, minBase, maxBase, baseDecimals, minPrice, maxPrice, priceDecimals);
        }

        public PairInfo withMaxBase(double maxBase) {
            return new PairInfo(pair, minBase, maxBase, baseDecimals, minPrice, maxPrice, priceDecimals);
        }

        public PairInfo withBaseDecimals(int baseDecimals) {
            return new PairInfo(pair, minBase, maxBase, baseDecimals, minPrice, maxPrice, priceDecimals);
        }

        public PairInfo withMinPrice(double minPrice) {
            return new PairInfo(pair, minBase, maxBase, baseDecimals, minPrice, maxPrice, priceDecimals);
        }

        public PairInfo withMaxPrice(double maxPrice) {
            return new PairInfo(pair, minBase, maxBase, baseDecimals, minPrice, maxPrice, priceDecimals);
        }

        public PairInfo withPriceDecimals(int priceDecimals) {
            return new PairInfo(pair, minBase, maxBase, baseDecimals, minPrice, maxPrice, priceDecimals);
        }
    }
}
