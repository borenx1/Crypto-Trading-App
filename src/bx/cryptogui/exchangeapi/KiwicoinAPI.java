package bx.cryptogui.exchangeapi;

import bx.cryptogui.data.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KiwicoinAPI extends ExchangeAPI {

    public KiwicoinAPI() {
        super();
    }

    public KiwicoinAPI(Authorisation authorisation) {
        super(authorisation);
    }

    public KiwicoinAPI(String key, String secret) {
        super(key, secret);
    }

    @Override
    public Exchange getExchange() {
        return null;
    }

    /**
     * Only BTC/USD is available on kiwicoin.
     * @param currencyPair currency pair
     * @return null
     */
    @Override
    public String convertCurrencyPair(CurrencyPair currencyPair) {
        return null;
    }

    @Override
    public Ticker getTicker(CurrencyPair currencyPair) throws IOException, HTTPException {
        if (!currencyPair.equals(CurrencyPair.BTC_NZD)) {
            return null;
        }
        Map<String, String> headers = new HashMap<>();      // must have user-agent, else forbidden
        headers.put("User-Agent", "");
        HTTPResponse response = getRequest(new URL("https://kiwi-coin.com/api/ticker"), headers);
        if (response.getResponseCode() == 200) {
            JSONObject json = response.getJSONObject();
            long time = json.getLong("date");
            double last = json.getDouble("last");
            int type = last >= json.getDouble("ask") ? 0 : (last <= json.getDouble("bid") ? 1 : -1);
            double high = json.getDouble("high");
            double low = json.getDouble("low");
            double avg = json.getDouble("vwap");
            double volume = json.getDouble("volume");
            return new Ticker(time, last, type, high, low, avg, volume);
        } else {
            throw new HTTPException(response);
        }
    }

    @Override
    public List<Order>[] getOrderBook(CurrencyPair currencyPair) throws IOException, HTTPException {
        if (!currencyPair.equals(CurrencyPair.BTC_NZD)) {
            return null;
        }
        List<Order>[] orders = new List[] {new ArrayList<>(), new ArrayList<>()};
        Map<String, String> headers = new HashMap<>();      // must have user-agent, else forbidden
        headers.put("User-Agent", "");
        HTTPResponse response = getRequest(new URL("https://kiwi-coin.com/api/order_book"), headers);
        if (response.getResponseCode() == 200) {
            JSONObject json = response.getJSONObject();
            JSONArray bids = json.getJSONArray("bids");
            JSONArray asks = json.getJSONArray("asks");
            for (int i = 0; i < bids.length(); i++) {
                JSONArray array = bids.getJSONArray(i);
                double price = array.getDouble(0);
                double volume = array.getDouble(1);
                orders[0].add(new Order(-1, -1, price, volume, true, new TradePlatform(Exchange.KIWICOIN, currencyPair)));
            }
            for (int i = 0; i < asks.length(); i++) {
                JSONArray array = asks.getJSONArray(i);
                double price = array.getDouble(0);
                double volume = array.getDouble(1);
                orders[1].add(new Order(-1, -1, price, volume, false, new TradePlatform(Exchange.KIWICOIN, currencyPair)));
            }
        } else {
            throw new HTTPException(response);
        }
        return orders;
    }

    /**
     * No trade data.
     * @param currencyPair currency pair
     * @return null
     */
    @Override
    public List<Transaction> getTrades(CurrencyPair currencyPair) {
        return null;
    }

    /**
     * No trade data.
     * @return Empty map
     */
    @Override
    public Map<TradePlatform, List<Transaction>> getAllTrades() {
        return new HashMap<>();
    }

    @Override
    public Map<Currency, Double> getBalance() throws AuthorisationInvalidException, IOException, HTTPException {
        return null;
    }

    @Override
    public List<Order> getOpenOrders() throws AuthorisationInvalidException, IOException, HTTPException {
        return null;
    }

    @Override
    public List<Transaction> getTradeHistory() throws AuthorisationInvalidException, IOException, HTTPException {
        return null;
    }
}
