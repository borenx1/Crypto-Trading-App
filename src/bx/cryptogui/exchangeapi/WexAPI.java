package bx.cryptogui.exchangeapi;

import bx.cryptogui.data.*;
import bx.cryptogui.data.Currency;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;

public class WexAPI extends ExchangeAPI {

    public static final String INVALID_API_KEY = "invalid api key";
    public static final String INVALID_SIGN = "invalid sign";

    public WexAPI() {
        super();
    }

    public WexAPI(Authorisation authorisation) {
        super(authorisation);
    }

    public WexAPI(String key, String secret) {
        super(key, secret);
    }

    @Override
    public Exchange getExchange() {
        return Exchange.WEX;
    }

    @Override
    public String convertCurrencyPair(CurrencyPair currencyPair) {
        if (currencyPair.equals(CurrencyPair.BTC_USD)) {
            return "btc_usd";
        } else if (currencyPair.equals(CurrencyPair.LTC_USD)) {
            return "ltc_usd";
        } else if (currencyPair.equals(CurrencyPair.ETH_USD)) {
            return "eth_usd";
        }
        return null;
    }

    @Override
    public Ticker getTicker(CurrencyPair currencyPair) throws IOException, HTTPException {
        String pairString = convertCurrencyPair(currencyPair);
        if (pairString == null) return null;
        HTTPResponse response = getRequest(new URL("https://wex.nz/api/3/ticker/" + pairString));
        if (response.getResponseCode() == 200) {
            JSONObject json = response.getJSONObject().getJSONObject(pairString);
            long time = json.getLong("updated");
            double last = json.getDouble("last");
            int type = last == json.getDouble("buy") ? 0 : last == json.getDouble("sell") ? 1 : -1;
            double high = json.getDouble("high");
            double low = json.getDouble("low");
            double avg = json.getDouble("avg");
            double volume = json.getDouble("vol_cur");
            return new Ticker(time, last, type, high, low, avg, volume);
        } else {
            throw new HTTPException(response);
        }
    }

    @Override
    public List<Order>[] getOrderBook(CurrencyPair currencyPair) throws IOException, HTTPException {
        List<Order>[] orders = new List[] {new ArrayList<Order>(), new ArrayList<Order>()};
        String pairString = convertCurrencyPair(currencyPair);
        if (pairString == null) return null;
        HTTPResponse response = getRequest(new URL("https://wex.nz/api/3/depth/" + pairString + "?limit=200"));
        if (response.getResponseCode() == 200) {
            JSONObject json = response.getJSONObject();
            JSONArray asks = json.getJSONObject(pairString).getJSONArray("asks");
            JSONArray bids = json.getJSONObject(pairString).getJSONArray("bids");
            for (int i = 0; i < bids.length(); i++) {
                JSONArray array = bids.getJSONArray(i);
                double price = array.getDouble(0);
                double volume = array.getDouble(1);
                orders[0].add(new Order(-1, -1, price, volume, true, new TradePlatform(Exchange.WEX, currencyPair)));
            }
            for (int i = 0; i < asks.length(); i++) {
                JSONArray array = asks.getJSONArray(i);
                double price = array.getDouble(0);
                double volume = array.getDouble(1);
                orders[1].add(new Order(-1, -1, price, volume, false, new TradePlatform(Exchange.WEX, currencyPair)));
            }
        } else {
            throw new HTTPException(response);
        }
        return orders;
    }

    @Override
    public List<Transaction> getTrades(CurrencyPair currencyPair) throws IOException, HTTPException {
        List<Transaction> trades = new ArrayList<>();
        String pairString = convertCurrencyPair(currencyPair);
        if (pairString == null) return null;
        HTTPResponse response = getRequest(new URL("https://wex.nz/api/3/trades/" + pairString + "?limit=5000"));
        if (response.getResponseCode() == 200) {
            JSONObject json = response.getJSONObject();
            JSONArray array = json.getJSONArray(pairString);
            for (int i = 0; i < array.length(); i++) {
                JSONObject transaction = array.getJSONObject(i);
                long id = transaction.getLong("tid");
                long time = transaction.getLong("timestamp");
                double price = transaction.getDouble("price");
                double volume = transaction.getDouble("amount");
                int type = transaction.getString("type").equals("bid") ? 0 : 1;
                trades.add(new Transaction(id, time, price, volume, type, new TradePlatform(Exchange.WEX, currencyPair)));
            }
        } else {
            throw new HTTPException(response);
        }
        return trades;
    }

    @Override
    public Map<TradePlatform, List<Transaction>> getAllTrades() throws IOException, HTTPException {
        Map<TradePlatform, List<Transaction>> trades = new HashMap<>();
        trades.put(TradePlatform.WEX_BTC_USD, getTrades(CurrencyPair.BTC_USD));
        trades.put(new TradePlatform(Exchange.WEX, CurrencyPair.LTC_USD), getTrades(CurrencyPair.LTC_USD));
        trades.put(new TradePlatform(Exchange.WEX, CurrencyPair.ETH_USD), getTrades(CurrencyPair.ETH_USD));
        return trades;
    }

    @Override
    public Map<Currency, Double> getBalance() throws AuthorisationInvalidException, IOException, IllegalArgumentException {
        if (!isAuthorised()) {
            throw new IllegalArgumentException("Not authenticated");
        }
        Map<String, String> parameters = new HashMap<>();
        parameters.put("method", "getInfo");
        parameters.put("nonce", String.valueOf(getNonce()));

        Map<String, String> headers = new HashMap<>();
        headers.put("Key", getKey());
        headers.put("Sign", getSignature(urlEncode(parameters)));

        HTTPResponse response = postRequest(new URL("https://wex.nz/tapi"), headers, parameters);
        Map<Currency, Double> balance = new HashMap<>();
        if (response.getResponseCode() >= 400) {
            System.err.println("response: " + response);
        } else {
            JSONObject json = response.getJSONObject();
            if (json.getInt("success") == 0) {  // failed
                String error = json.getString("error");
                System.err.println(error);
                switch (error) {
                    case INVALID_API_KEY:
                        throw new AuthorisationInvalidException(INVALID_API_KEY);
                    case INVALID_SIGN:
                        throw new AuthorisationInvalidException(INVALID_SIGN);
                    default:
                        throw new AuthorisationInvalidException();
                }
            } else {
                JSONObject funds = json.getJSONObject("return").getJSONObject("funds");
                for (String key: new String[] {"btc", "usd", "ltc"}) {
                    balance.put(Currency.valueOf(key.toUpperCase()), funds.getDouble(key));
                }
            }
        }
        return balance;
    }

    @Override
    public List<Order> getOpenOrders() throws AuthorisationInvalidException, IOException {
        return new ArrayList<>();
    }

    @Override
    public List<Transaction> getTradeHistory() throws AuthorisationInvalidException, IOException {
        return new ArrayList<>();
    }
}
