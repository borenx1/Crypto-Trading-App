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

public class CryptopiaAPI extends ExchangeAPI {

    public CryptopiaAPI() {
        super();
    }

    public CryptopiaAPI(Authorisation authorisation) {
        super(authorisation);
    }

    public CryptopiaAPI(String key, String secret) {
        super(key, secret);
    }

    @Override
    public Exchange getExchange() {
        return null;
    }

    @Override
    public String convertCurrencyPair(CurrencyPair currencyPair) {
        if (currencyPair.equals(CurrencyPair.BTC_USD)) {
            return "BTC_USDT";
        } else if (currencyPair.equals(CurrencyPair.BTC_NZD)) {
            return "BTC_NZDT";
        } else if (currencyPair.equals(CurrencyPair.LTC_USD)) {
            return "LTC_USDT";
        }else if (currencyPair.equals(CurrencyPair.LTC_NZD)) {
            return "LTC_NZDT";
        }else if (currencyPair.equals(CurrencyPair.ETH_USD)) {
            return "ETH_USDT";
        }else if (currencyPair.equals(CurrencyPair.ETH_NZD)) {
            return "ETH_NZDT";
        }
        return null;
    }

    @Override
    public Ticker getTicker(CurrencyPair currencyPair) throws IOException, HTTPException {
        String pairString = convertCurrencyPair(currencyPair);
        if (pairString == null) return null;
        HTTPResponse response = getRequest(new URL("https://www.cryptopia.co.nz/api/GetMarket/" + pairString));
        if (response.getResponseCode() == 200) {
            JSONObject json = response.getJSONObject().getJSONObject("Data");
            long time = System.currentTimeMillis()/1000;
            double last = json.getDouble("LastPrice");
            int type = last == json.getDouble("AskPrice") ? 0 : (last == json.getDouble("BidPrice") ? 1 : -1);
            double high = json.getDouble("High");
            double low = json.getDouble("Low");
            double volume = json.getDouble("Volume");
            return new Ticker(time, last, type, high, low, -1, volume);
        } else {
            throw new HTTPException(response);
        }
    }

    @Override
    public List<Order>[] getOrderBook(CurrencyPair currencyPair) throws IOException, HTTPException {
        List<Order>[] orders = new List[] {new ArrayList<>(), new ArrayList<>()};
        String pairString = convertCurrencyPair(currencyPair);
        if (pairString == null) return null;
        HTTPResponse response = getRequest(new URL("https://www.cryptopia.co.nz/api/GetMarketOrders/" + pairString));
        if (response.getResponseCode() == 200) {
            JSONObject json = response.getJSONObject();
            JSONArray bids = json.getJSONObject("Data").getJSONArray("Buy");
            JSONArray asks = json.getJSONObject("Data").getJSONArray("Sell");
            for (int i = 0; i < bids.length(); i++) {
                JSONObject order = bids.getJSONObject(i);
                double price = order.getDouble("Price");
                double volume = order.getDouble("Volume");
                orders[0].add(new Order(-1, -1, price, volume, true, new TradePlatform(Exchange.CRYPTOPIA, currencyPair)));
            }
            for (int i = 0; i < asks.length(); i++) {
                JSONObject order = asks.getJSONObject(i);
                double price = order.getDouble("Price");
                double volume = order.getDouble("Volume");
                orders[1].add(new Order(-1, -1, price, volume, false, new TradePlatform(Exchange.CRYPTOPIA, currencyPair)));
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
        HTTPResponse response = getRequest(new URL("https://www.cryptopia.co.nz/api/GetMarketHistory/" + pairString));
        if (response.getResponseCode() == 200) {
            JSONObject json = response.getJSONObject();
            JSONArray array = json.getJSONArray("Data");
            for (int i = 0; i < array.length(); i++) {
                JSONObject transaction = array.getJSONObject(i);
                long time = transaction.getLong("Timestamp");
                double price = transaction.getDouble("Price");
                double volume = transaction.getDouble("Amount");
                int type = transaction.getString("Type").equals("Buy") ? 0 : 1;
                trades.add(new Transaction(-1, time, price, volume, type, new TradePlatform(Exchange.CRYPTOPIA, currencyPair)));
            }
        } else {
            throw new HTTPException(response);
        }
        return trades;
    }

    @Override
    public Map<TradePlatform, List<Transaction>> getAllTrades() throws IOException, HTTPException {
        Map<TradePlatform, List<Transaction>> trades = new HashMap<>();
        trades.put(TradePlatform.CRYPTOPIA_BTC_USD, getTrades(CurrencyPair.BTC_USD));
        trades.put(TradePlatform.CRYPTOPIA_BTC_NZD, getTrades(CurrencyPair.BTC_NZD));
        trades.put(new TradePlatform(Exchange.CRYPTOPIA, CurrencyPair.LTC_USD), getTrades(CurrencyPair.LTC_USD));
        trades.put(new TradePlatform(Exchange.CRYPTOPIA, CurrencyPair.LTC_NZD), getTrades(CurrencyPair.LTC_NZD));
        trades.put(new TradePlatform(Exchange.CRYPTOPIA, CurrencyPair.ETH_USD), getTrades(CurrencyPair.ETH_USD));
        trades.put(new TradePlatform(Exchange.CRYPTOPIA, CurrencyPair.ETH_NZD), getTrades(CurrencyPair.ETH_NZD));
        return trades;
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
