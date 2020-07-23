package bx.cryptogui.manager;

import bx.cryptogui.data.*;
import bx.cryptogui.exchangeapi.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.*;

public class APIManager {

    private final Map<Exchange, APIRetriever> exchanges = new HashMap<>();

    public APIManager() {
        exchanges.put(Exchange.WEX, new APIRetriever(Exchange.WEX, new WexAPI()));
        exchanges.put(Exchange.BITSTAMP, new APIRetriever(Exchange.BITSTAMP, new BitstampAPI()));
        exchanges.put(Exchange.COINBASE, new APIRetriever(Exchange.COINBASE, new CoinbaseAPI()));
        exchanges.put(Exchange.CRYPTOPIA, new APIRetriever(Exchange.CRYPTOPIA, new CryptopiaAPI()));
        exchanges.put(Exchange.KIWICOIN, new APIRetriever(Exchange.KIWICOIN, new KiwicoinAPI()));
    }

    public final boolean supportsExchange(Exchange exchange) {
        return exchanges.containsKey(exchange);
    }

    public final Set<Exchange> getSupportedExchanges() {
        return Collections.unmodifiableSet(exchanges.keySet());
    }

    public final APIRetriever getAPIRetriever(Exchange exchange) {
        return exchanges.get(exchange);
    }

    public final List<APIRetriever> getAllAPIRetrievers() {
        return Collections.unmodifiableList(new ArrayList<>(exchanges.values()));
    }

    public void startRetrieving(TradePlatform platform) {
        APIRetriever retriever = exchanges.get(platform.getExchange());
        retriever.setCurrencyPair(platform.getCurrencyPair());
        retriever.restart();
    }


    public class APIRetriever extends Service<Void> {

        private ReadOnlyLongWrapper lastUpdated = new ReadOnlyLongWrapper(Long.MIN_VALUE);
        public final long getLastUpdated() {
            return lastUpdated.get();
        }
        public final ReadOnlyLongProperty lastUpdatedProperty() {
            return lastUpdated.getReadOnlyProperty();
        }

        private ReadOnlyObjectWrapper<Ticker> ticker = new ReadOnlyObjectWrapper<>();
        public final Ticker getTicker() {
            return ticker.get();
        }
        public final ReadOnlyObjectProperty<Ticker> tickerProperty() {
            return ticker.getReadOnlyProperty();
        }

        private ReadOnlyObjectWrapper<List<Order>> bidOrders = new ReadOnlyObjectWrapper<>();
        public final List<Order> getBidOrders() {
            return bidOrders.get();
        }
        public final ReadOnlyObjectProperty<List<Order>> bidOrdersProperty() {
            return bidOrders.getReadOnlyProperty();
        }

        private ReadOnlyObjectWrapper<List<Order>> askOrders = new ReadOnlyObjectWrapper<>();
        public final List<Order> getAskOrders() {
            return askOrders.get();
        }
        public final ReadOnlyObjectProperty<List<Order>> askOrdersProperty() {
            return askOrders.getReadOnlyProperty();
        }

        private ObjectProperty<CurrencyPair> currencyPair = new SimpleObjectProperty<>();
        public final CurrencyPair getCurrencyPair() {
            return currencyPair.get();
        }
        public final void setCurrencyPair(CurrencyPair value) {
            currencyPair.set(value);
        }
        public final ObjectProperty<CurrencyPair> currencyPairProperty() {
            return currencyPair;
        }

        private final Exchange exchange;
        private final ExchangeAPI api;

        public APIRetriever(Exchange exchange, ExchangeAPI api) {
            this.exchange = Objects.requireNonNull(exchange);
            this.api = Objects.requireNonNull(api);
        }

        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    CurrencyPair pair = getCurrencyPair();
                    final Ticker newTicker = api.getTicker(pair);
                    final List<Order>[] orderBook = api.getOrderBook(pair);
                    Platform.runLater(() -> ticker.set(newTicker));
                    Platform.runLater(() -> bidOrders.set(orderBook[0]));
                    Platform.runLater(() -> askOrders.set(orderBook[1]));
                    Platform.runLater(() -> lastUpdated.set(newTicker.getTime()));
                    return null;
                }
            };
        }
    }

}
