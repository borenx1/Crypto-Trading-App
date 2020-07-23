package bx.cryptogui.exchangeapi;

import bx.cryptogui.data.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoinbaseAPI extends ExchangeAPI {

    public CoinbaseAPI() {
        super();
    }

    public CoinbaseAPI(Authorisation authorisation) {
        super(authorisation);
    }

    public CoinbaseAPI(String key, String secret) {
        super(key, secret);
    }

    @Override
    public Exchange getExchange() {
        return Exchange.COINBASE;
    }

    @Override
    public String convertCurrencyPair(CurrencyPair currencyPair) {
        if (currencyPair.equals(CurrencyPair.BTC_USD)) {
            return "BTC-USD";
        } else if (currencyPair.equals(CurrencyPair.LTC_USD)) {
            return "LTC-USD";
        } else if (currencyPair.equals(CurrencyPair.ETH_USD)) {
            return "ETH-USD";
        }
        return null;
    }

    @Override
    public Ticker getTicker(CurrencyPair currencyPair) throws IOException, HTTPException {
        String pairString = convertCurrencyPair(currencyPair);
        if (pairString == null) return null;
        HTTPResponse response = getRequest(new URL("https://api.gdax.com/products/" + pairString + "/ticker"));
        if (response.getResponseCode() == 200) {
            JSONObject json = response.getJSONObject();
            long time = DatatypeConverter.parseDateTime(json.getString("time")).toInstant().getEpochSecond();
            double last = json.getDouble("price");
            int type = last == json.getDouble("ask") ? 0 : (last == json.getDouble("bid") ? 1 : -1);
            double volume = json.getDouble("volume");
            return new Ticker(time, last, type, -1, -1, -1, volume);
        } else {
            throw new HTTPException(response);
        }
    }

    @Override
    public List<Order>[] getOrderBook(CurrencyPair currencyPair) throws IOException, HTTPException {
        List<Order>[] orders = new List[] {new ArrayList<>(), new ArrayList<>()};
        String pairString = convertCurrencyPair(currencyPair);
        if (pairString == null) return null;
        HTTPResponse response = getRequest(new URL("https://api.gdax.com/products/" + pairString + "/book?level=2"));
        if (response.getResponseCode() == 200) {
            JSONObject json = response.getJSONObject();
            JSONArray bids = json.getJSONArray("bids");
            JSONArray asks = json.getJSONArray("asks");
            for (int i = 0; i < bids.length(); i++) {
                JSONArray array = bids.getJSONArray(i);
                double price = array.getDouble(0);
                double volume = array.getDouble(1);
                orders[0].add(new Order(-1, -1, price, volume, true, new TradePlatform(Exchange.COINBASE, currencyPair)));
            }
            for (int i = 0; i < asks.length(); i++) {
                JSONArray array = asks.getJSONArray(i);
                double price = array.getDouble(0);
                double volume = array.getDouble(1);
                orders[1].add(new Order(-1, -1, price, volume, false, new TradePlatform(Exchange.COINBASE, currencyPair)));
            }
        } else {
            throw new HTTPException(response);
        }
        return orders;
    }

    public List<Transaction> getTrades(CurrencyPair currencyPair, long minTime) throws IOException, HTTPException {
        List<Transaction> trades = new ArrayList<>();
        String pairString = convertCurrencyPair(currencyPair);
        if (pairString == null) return null;
        // Start pagination loop, 100 per page, get 10 pages
        int count = 0;
        String urlString = "https://api.gdax.com/products/" + pairString + "/trades";   // Init first page
        do {
            HTTPResponse response = getRequest(new URL(urlString));
            if (response.getResponseCode() == 200) {
                int page = Integer.valueOf(response.getHeaderField("cb-after").get(0));
                JSONArray json = response.getJSONArray();
                for (int i = 0; i < json.length(); i++) {
                    JSONObject transaction = json.getJSONObject(i);
                    long id = transaction.getLong("trade_id");
                    long time = DatatypeConverter.parseDateTime(transaction.getString("time")).toInstant().getEpochSecond();
                    double price = transaction.getDouble("price");
                    double volume = transaction.getDouble("size");
                    int type = transaction.getString("side").equals("buy") ? 1 : 0;     // OPPOSITE, 'side' is maker order side
                    trades.add(new Transaction(id, time, price, volume, type, new TradePlatform(Exchange.COINBASE, currencyPair)));
                }
                // Update URL!!!
                urlString = "https://api.gdax.com/products/" + pairString + "/trades?after=" + page;
            } else {
                throw new HTTPException(response);
            }
        } while (count++ < 50 && trades.get(trades.size()-1).getTime() >= minTime);
        return trades;
    }

    @Override
    public List<Transaction> getTrades(CurrencyPair currencyPair) throws IOException, HTTPException {
        return getTrades(currencyPair, Long.MIN_VALUE);
    }

    @Override
    public Map<TradePlatform, List<Transaction>> getAllTrades() throws IOException, HTTPException {
        Map<TradePlatform, List<Transaction>> trades = new HashMap<>();
        trades.put(TradePlatform.COINBASE_BTC_USD, getTrades(CurrencyPair.BTC_USD));
        trades.put(new TradePlatform(Exchange.COINBASE, CurrencyPair.LTC_USD), getTrades(CurrencyPair.LTC_USD));
        trades.put(new TradePlatform(Exchange.COINBASE, CurrencyPair.ETH_USD), getTrades(CurrencyPair.ETH_USD));
        return trades;
    }

    @Override
    public Map<Currency, Double> getBalance() throws AuthorisationInvalidException, IOException {
        return null;
    }

    @Override
    public List<Order> getOpenOrders() throws AuthorisationInvalidException, IOException {
        return null;
    }

    @Override
    public List<Transaction> getTradeHistory() throws AuthorisationInvalidException, IOException {
        return null;
    }

    @Override
    public String getSignature(byte[] data) {
        return super.getSignature(data);
    }
}
