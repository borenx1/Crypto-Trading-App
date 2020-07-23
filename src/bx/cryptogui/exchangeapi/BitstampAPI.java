package bx.cryptogui.exchangeapi;

import bx.cryptogui.data.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BitstampAPI extends ExchangeAPI {

    public BitstampAPI() {
        super();
    }

    public BitstampAPI(Authorisation authorisation) {
        super(authorisation);
    }

    public BitstampAPI(String key, String secret) {
        super(key, secret);
    }

    @Override
    public Exchange getExchange() {
        return Exchange.BITSTAMP;
    }

    @Override
    public String convertCurrencyPair(CurrencyPair currencyPair) {
        if (currencyPair.equals(CurrencyPair.BTC_USD)) {
            return "btcusd";
        } else if (currencyPair.equals(CurrencyPair.LTC_USD)) {
            return "ltcusd";
        } else if (currencyPair.equals(CurrencyPair.ETH_USD)) {
            return "ethusd";
        }
        return null;
    }

    @Override
    public Ticker getTicker(CurrencyPair currencyPair) throws IOException, HTTPException {
        String pairString = convertCurrencyPair(currencyPair);
        if (pairString == null) return null;
        HTTPResponse response = getRequest(new URL("https://www.bitstamp.net/api/v2/ticker/" + pairString));
        if (response.getResponseCode() == 200) {
            JSONObject json = response.getJSONObject();
            long time = json.getLong("timestamp");
            double last = json.getDouble("last");
            int type = last == json.getDouble("ask") ? 0 : (last == json.getDouble("bid") ? 1 : -1);
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
        List<Order>[] orders = new List[] {new ArrayList<>(), new ArrayList<>()};
        String pairString = convertCurrencyPair(currencyPair);
        if (pairString == null) return null;
        HTTPResponse response = getRequest(new URL("https://www.bitstamp.net/api/v2/order_book/" + pairString));
        if (response.getResponseCode() == 200) {
            JSONObject json = response.getJSONObject();
            JSONArray bids = json.getJSONArray("bids");
            JSONArray asks = json.getJSONArray("asks");
            for (int i = 0; i < bids.length(); i++) {
                JSONArray array = bids.getJSONArray(i);
                double price = array.getDouble(0);
                double volume = array.getDouble(1);
                orders[0].add(new Order(-1, -1, price, volume, true, new TradePlatform(Exchange.BITSTAMP, currencyPair)));
            }
            for (int i = 0; i < asks.length(); i++) {
                JSONArray array = asks.getJSONArray(i);
                double price = array.getDouble(0);
                double volume = array.getDouble(1);
                orders[1].add(new Order(-1, -1, price, volume, false, new TradePlatform(Exchange.BITSTAMP, currencyPair)));
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
        HTTPResponse response = getRequest(new URL("https://www.bitstamp.net/api/v2/transactions/" + pairString));
        if (response.getResponseCode() == 200) {
            JSONArray json = response.getJSONArray();
            for (int i = 0; i < json.length(); i++) {
                JSONObject transaction = json.getJSONObject(i);
                long id = transaction.getLong("tid");
                long time = transaction.getLong("date");
                double price = transaction.getDouble("price");
                double volume = transaction.getDouble("amount");
                int type = transaction.getInt("type");
                trades.add(new Transaction(id, time, price, volume, type, new TradePlatform(Exchange.BITSTAMP, currencyPair)));
            }
        } else {
            throw new HTTPException(response);
        }
        return trades;
    }

    @Override
    public Map<TradePlatform, List<Transaction>> getAllTrades() throws IOException, HTTPException {
        Map<TradePlatform, List<Transaction>> trades = new HashMap<>();
        trades.put(TradePlatform.BITSTAMP_BTC_USD, getTrades(CurrencyPair.BTC_USD));
        trades.put(new TradePlatform(Exchange.BITSTAMP, CurrencyPair.LTC_USD), getTrades(CurrencyPair.LTC_USD));
        trades.put(new TradePlatform(Exchange.BITSTAMP, CurrencyPair.ETH_USD), getTrades(CurrencyPair.ETH_USD));
        return trades;
    }

    @Override
    public Map<Currency, Double> getBalance() throws AuthorisationInvalidException, IOException {
        return new HashMap<>();
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
