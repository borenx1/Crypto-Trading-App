package bx.cryptogui.manager;

import bx.cryptogui.Utils;
import bx.cryptogui.control.CandleStickValues;
import bx.cryptogui.data.CurrencyPair;
import bx.cryptogui.data.Exchange;
import bx.cryptogui.data.TradePlatform;
import bx.cryptogui.data.Transaction;
import bx.cryptogui.exchangeapi.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.chart.XYChart;

import java.sql.*;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

public class DatabaseManager {

    private final Connection connection;
    private final Map<Exchange, ExchangeManager> exchanges = new HashMap<>();

    public DatabaseManager(Connection connection) {
        this.connection = Objects.requireNonNull(connection);
        // hard-coded default
        Map<CurrencyPair, String> wexMap = new HashMap<>();
        wexMap.put(CurrencyPair.BTC_USD, "wex_btc_usd");
        wexMap.put(CurrencyPair.LTC_USD, "wex_ltc_usd");
        wexMap.put(CurrencyPair.ETH_USD, "wex_eth_usd");
        exchanges.put(Exchange.WEX, new ExchangeManager(Exchange.WEX, new WexAPI(), wexMap));

        Map<CurrencyPair, String> bitstampMap = new HashMap<>();
        bitstampMap.put(CurrencyPair.BTC_USD, "bitstamp_btc_usd");
        bitstampMap.put(CurrencyPair.LTC_USD, "bitstamp_ltc_usd");
        bitstampMap.put(CurrencyPair.ETH_USD, "bitstamp_eth_usd");
        exchanges.put(Exchange.BITSTAMP, new ExchangeManager(Exchange.BITSTAMP, new BitstampAPI(), bitstampMap));

        Map<CurrencyPair, String> coinbaseMap = new HashMap<>();
        coinbaseMap.put(CurrencyPair.BTC_USD, "coinbase_btc_usd");
        coinbaseMap.put(CurrencyPair.LTC_USD, "coinbase_ltc_usd");
        coinbaseMap.put(CurrencyPair.ETH_USD, "coinbase_eth_usd");
        exchanges.put(Exchange.COINBASE, new ExchangeManager(Exchange.COINBASE, new CoinbaseAPI(), coinbaseMap));

        Map<CurrencyPair, String> cryptopiaMap = new HashMap<>();
        cryptopiaMap.put(CurrencyPair.BTC_USD, "cryptopia_btc_usd");
        cryptopiaMap.put(CurrencyPair.BTC_NZD, "cryptopia_btc_nzd");
        cryptopiaMap.put(CurrencyPair.LTC_USD, "cryptopia_ltc_usd");
        cryptopiaMap.put(CurrencyPair.LTC_NZD, "cryptopia_ltc_nzd");
        cryptopiaMap.put(CurrencyPair.ETH_USD, "cryptopia_eth_usd");
        cryptopiaMap.put(CurrencyPair.ETH_NZD, "cryptopia_eth_nzd");
        exchanges.put(Exchange.CRYPTOPIA, new ExchangeManager(Exchange.CRYPTOPIA, new CryptopiaAPI(), cryptopiaMap));

        Map<CurrencyPair, String> kiwicoinMap = new HashMap<>();
        kiwicoinMap.put(CurrencyPair.BTC_NZD, "kiwicoin_btc_nzd");
        exchanges.put(Exchange.KIWICOIN, new ExchangeManager(Exchange.KIWICOIN, new KiwicoinAPI(), kiwicoinMap));
    }

    public final boolean supportsExchange(Exchange exchange) {
        return exchanges.containsKey(exchange);
    }

    public final Set<Exchange> getSupportedExchanges() {
        return Collections.unmodifiableSet(exchanges.keySet());
    }

    public final ExchangeManager getExchangeManager(Exchange exchange) {
        return exchanges.get(exchange);
    }

    public final List<ExchangeManager> getAllExchangeManagers() {
        return Collections.unmodifiableList(new ArrayList<>(exchanges.values()));
    }



    public void requestDatabaseUpdate() {
        for (ExchangeManager exchangeManager : exchanges.values()) {
            exchangeManager.startWritingIfFinished();
        }
    }

    public void startReadingThenCalculate(TradePlatform platform) {
        exchanges.get(platform.getExchange()).startReadingThenCalculate(platform.getCurrencyPair());
    }

    public void startCalculating(TradePlatform platform) {
        exchanges.get(platform.getExchange()).startCalculating(platform.getCurrencyPair());
    }

    // ------------------ DATABASE METHODS -----------------------------------------------------------------------------

    protected long getLatestTime(String tableName) throws SQLException {
        String SQLString = String.format("SELECT MAX(time) FROM %s", tableName);
        try (PreparedStatement statement = connection.prepareStatement(SQLString)) {
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    // Synchronised methods, can only have 1 writer writing at a time,
    // prevent 'cannot commit - no transaction is active'
    protected synchronized int writeToDatabase(String tableName, List<Transaction> transactions) throws SQLException {
        // FIRST: find latest time in database, don't re-insert same rows
        long maxDatabaseTime = getLatestTime(tableName);
        deleteRowsAtLeastTime(tableName, maxDatabaseTime);   // re-insert rows of last time
        // only insert rows with time AT LEAST max time in database (no re-inserting earlier rows)
        List<Transaction> toInsert = new ArrayList<>(transactions);
        Collections.reverse(toInsert);  // reverse to get ascending time
        Collections.sort(toInsert);     // sort just in case
        int index = toInsert.size();
        for (int i = 0; i < toInsert.size(); i++) {
            if (toInsert.get(i).getTime() >= maxDatabaseTime) {
                index = i;
                break;
            }
        }
        toInsert = toInsert.subList(index, toInsert.size());
        connection.setAutoCommit(false);
        // Columns: time(INT), price(REAL), volume(REAL), type(INT)
        String SQLString = String.format("INSERT INTO %s VALUES (?, ?, ?, ?, ?)", tableName);
        try (PreparedStatement statement = connection.prepareStatement(SQLString)) {
            for (Transaction trans: toInsert) {
                statement.setLong(1, trans.getId());
                statement.setLong(2, trans.getTime());
                statement.setDouble(3, trans.getPrice());
                statement.setDouble(4, trans.getVolume());
                statement.setInt(5, trans.getTradeType());
                statement.addBatch();
            }
            int[] results = statement.executeBatch();
            connection.commit();
            return results.length;
        }
    }

    protected void deleteRowsAtLeastTime(String tableName, long time) throws SQLException {
        String SQLString = String.format("DELETE FROM %s WHERE time >= ?", tableName);
        try (PreparedStatement statement = connection.prepareStatement(SQLString)) {
            statement.setLong(1, time);
            statement.executeUpdate();
        }
    }


    public class ExchangeManager {

        private ReadOnlyLongWrapper lastUpdated = new ReadOnlyLongWrapper(Long.MIN_VALUE);
        public final long getLastUpdated() {
            return lastUpdated.get();
        }
        public final ReadOnlyLongProperty lastUpdatedProperty() {
            return lastUpdated.getReadOnlyProperty();
        }

        private ReadOnlyIntegerWrapper newRows = new ReadOnlyIntegerWrapper(0);
        public final int getNewRows() {
            return newRows.get();
        }
        public final ReadOnlyIntegerProperty newRowsProperty() {
            return newRows.getReadOnlyProperty();
        }

        private LongProperty minTime = new SimpleLongProperty(this, "minTime", Long.MIN_VALUE);
        public final long getMinTime() {
            return minTime.get();
        }
        public final void setMinTime(long time) {
            minTime.set(time);
        }
        public final LongProperty minTimeProperty() {
            return minTime;
        }

        private LongProperty maxTime = new SimpleLongProperty(this, "maxTime", Long.MAX_VALUE);
        public final long getMaxTime() {
            return maxTime.get();
        }
        public final void setMaxTime(long time) {
            maxTime.set(time);
        }
        public final LongProperty maxTimeProperty() {
            return maxTime;
        }

        /**
         * Interval should be a multiple of minutes, hours, days or weeks. Also be divisible
         * by the maximum of the chosen unit, eg. 5m, 1h, 3d, 1w. NOT: 1m5s, 7m, 9h.
         */
        private IntegerProperty interval = new SimpleIntegerProperty(this, "interval", 3600);
        public final int getInterval() {
            return interval.get();
        }
        public final void setInterval(int interval) {
            this.interval.set(interval);
        }
        public final IntegerProperty intervalProperty() {
            return interval;
        }

        private ReadOnlyBooleanWrapper reading = new ReadOnlyBooleanWrapper(false);
        public final boolean isReading() {
            return reading.get();
        }
        public final ReadOnlyBooleanProperty readingProperty() {
            return reading.getReadOnlyProperty();
        }

        private ReadOnlyBooleanWrapper writing = new ReadOnlyBooleanWrapper(false);
        public final boolean isWriting() {
            return writing.get();
        }
        public final ReadOnlyBooleanProperty writingProperty() {
            return writing.getReadOnlyProperty();
        }

        private ReadOnlyBooleanWrapper calculating = new ReadOnlyBooleanWrapper(false);
        public final boolean isCalculating() {
            return calculating.get();
        }
        public final ReadOnlyBooleanProperty calculatingProperty() {
            return calculating.getReadOnlyProperty();
        }

        private ReadOnlyDoubleWrapper readingProgress = new ReadOnlyDoubleWrapper(-1);
        public final double getReadingProgress() {
            return readingProgress.get();
        }
        public final ReadOnlyDoubleProperty readingProgressProperty() {
            return readingProgress.getReadOnlyProperty();
        }

        private ReadOnlyDoubleWrapper calculatingProgress = new ReadOnlyDoubleWrapper(-1);
        public final double getCalculatingProgress() {
            return calculatingProgress.get();
        }
        public final ReadOnlyDoubleProperty calculatingProgressProperty() {
            return calculatingProgress.getReadOnlyProperty();
        }

        public final List<Transaction> getReaderValue() {
            return reader.getValue();
        }
        public final ReadOnlyObjectProperty<List<Transaction>> readerValueProperty() {
            return reader.valueProperty();
        }

        public final XYChart.Series<Number, Number>[] getCalculatorValue() {
            return calculator.getValue();
        }
        public final ReadOnlyObjectProperty<XYChart.Series<Number, Number>[]> calculatorValueProperty() {
            return calculator.valueProperty();
        }

        private ObjectProperty<EventHandler<WorkerStateEvent>> onWritingSucceeded = new SimpleObjectProperty<>();
        public final EventHandler<WorkerStateEvent> getOnWritingSucceeded() {
            return onWritingSucceeded.get();
        }
        public final void setOnWritingSucceeded(EventHandler<WorkerStateEvent> value) {
            onWritingSucceeded.set(value);
        }
        public final ObjectProperty<EventHandler<WorkerStateEvent>> onWritingSucceededProperty() {
            return onWritingSucceeded;
        }

        private ObjectProperty<EventHandler<WorkerStateEvent>> onReadingSucceeded = new SimpleObjectProperty<>();
        public final EventHandler<WorkerStateEvent> getOnReadingSucceeded() {
            return onReadingSucceeded.get();
        }
        public final void setOnReadingSucceeded(EventHandler<WorkerStateEvent> value) {
            onReadingSucceeded.set(value);
        }
        public final ObjectProperty<EventHandler<WorkerStateEvent>> onReadingSucceededProperty() {
            return onReadingSucceeded;
        }

        private ObjectProperty<EventHandler<WorkerStateEvent>> onCalculatingSucceeded = new SimpleObjectProperty<>();
        public final EventHandler<WorkerStateEvent> getOnCalculatingSucceeded() {
            return onCalculatingSucceeded.get();
        }
        public final void setOnCalculatingSucceeded(EventHandler<WorkerStateEvent> value) {
            onCalculatingSucceeded.set(value);
        }
        public final ObjectProperty<EventHandler<WorkerStateEvent>> onCalculatingSucceededProperty() {
            return onCalculatingSucceeded;
        }

        private ObjectProperty<EventHandler<WorkerStateEvent>> onWritingFailed = new SimpleObjectProperty<>();
        public final EventHandler<WorkerStateEvent> getOnWritingFailed() {
            return onWritingFailed.get();
        }
        public final void setOnWritingFailed(EventHandler<WorkerStateEvent> value) {
            onWritingFailed.set(value);
        }
        public final ObjectProperty<EventHandler<WorkerStateEvent>> onWritingFailedProperty() {
            return onWritingFailed;
        }

        private ObjectProperty<EventHandler<WorkerStateEvent>> onReadingFailed = new SimpleObjectProperty<>();
        public final EventHandler<WorkerStateEvent> getOnReadingFailed() {
            return onReadingFailed.get();
        }
        public final void setOnReadingFailed(EventHandler<WorkerStateEvent> value) {
            onReadingFailed.set(value);
        }
        public final ObjectProperty<EventHandler<WorkerStateEvent>> onReadingFailedProperty() {
            return onReadingFailed;
        }

        private ObjectProperty<EventHandler<WorkerStateEvent>> onCalculatingFailed = new SimpleObjectProperty<>();
        public final EventHandler<WorkerStateEvent> getOnCalculatingFailed() {
            return onCalculatingFailed.get();
        }
        public final void setOnCalculatingFailed(EventHandler<WorkerStateEvent> value) {
            onCalculatingFailed.set(value);
        }
        public final ObjectProperty<EventHandler<WorkerStateEvent>> onCalculatingFailedProperty() {
            return onCalculatingFailed;
        }

        private final Exchange exchange;
        private final ExchangeAPI api;
        private final Map<CurrencyPair, String> tableNames = new HashMap<>();
        private final WriterService writer = new WriterService();
        private final ReaderService reader = new ReaderService();
        private final Calculator calculator = new Calculator();

        protected ExchangeManager(Exchange exchange, ExchangeAPI api, Map<CurrencyPair, String> tableNames) {
            this.exchange = Objects.requireNonNull(exchange);
            this.api = Objects.requireNonNull(api);
            this.tableNames.putAll(tableNames);

            writing.bind(writer.runningProperty());
            writer.onSucceededProperty().bind(onWritingSucceeded);
            writer.onFailedProperty().bind(onWritingFailed);

            reading.bind(reader.runningProperty());
            readingProgress.bind(reader.progressProperty());
            reader.onSucceededProperty().bind(onReadingSucceeded);
            reader.onFailedProperty().bind(onReadingFailed);

            calculating.bind(calculator.runningProperty());
            calculatingProgress.bind(calculator.progressProperty());
            calculator.onSucceededProperty().bind(onCalculatingSucceeded);
            calculator.onFailedProperty().bind(onCalculatingFailed);
        }

        public void startWriting() {
            writer.restart();
        }

        public void startWritingIfFinished() {
            if (!writer.isRunning()) {
                startWriting();
            }
        }

        public void startReadingThenCalculate(CurrencyPair pair) {
            reader.setCurrencyPair(pair);
            reader.restart();   // Calculator auto starts when succeeded
        }

        public void startCalculating(CurrencyPair pair) {
            List<Transaction> trades = getReaderValue();
            if (trades == null || !reader.getCurrencyPair().equals(pair) || trades.isEmpty() ||
                    trades.get(0).getTime() >= getMinTime()) {
                startReadingThenCalculate(pair);
            } else {
                calculator.restart();
            }
        }

        public class WriterService extends Service<Void> {

            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        long latestTime = Long.MIN_VALUE;
                        int rowsAdded = 0;
                        if (api instanceof CoinbaseAPI) {
                            for (CurrencyPair pair: tableNames.keySet()) {
                                long minTime = getLatestTime(tableNames.get(pair));
                                List<Transaction> trades = ((CoinbaseAPI) api).getTrades(pair, minTime);
                                if (trades != null && !trades.isEmpty()) {
                                    latestTime = Math.max(trades.get(0).getTime(), latestTime);
                                    rowsAdded += writeToDatabase(tableNames.get(pair), trades);
                                }
                            }
                        } else {
                            Map<TradePlatform, List<Transaction>> allTrades = api.getAllTrades();
                            for (CurrencyPair pair: tableNames.keySet()) {
                                List<Transaction> trades = allTrades.get(new TradePlatform(exchange, pair));
                                if (trades != null && !trades.isEmpty()) {
                                    latestTime = Math.max(trades.get(0).getTime(), latestTime);
                                    // Actual writing to SQLite database
                                    rowsAdded += writeToDatabase(tableNames.get(pair), trades);
                                }
                            }
                        }
                        final long finalLatestTime = latestTime;
                        final int finalRowsAdded = rowsAdded;
                        Platform.runLater(() -> lastUpdated.set(finalLatestTime));
                        Platform.runLater(() -> newRows.set(finalRowsAdded));
                        return null;
                    }
                };
            }
        }

        protected class ReaderService extends Service<List<Transaction>> {

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

            @Override
            protected Task<List<Transaction>> createTask() {
                final CurrencyPair pair = getCurrencyPair();
                final TradePlatform platform = new TradePlatform(exchange, pair);
                final String tableName = tableNames.get(pair);
                if (tableName == null) {    // fail if table does't exists
                    return new Task<List<Transaction>>() {
                        @Override
                        protected List<Transaction> call() throws NullPointerException {
                            throw new NullPointerException("No table for " + getCurrencyPair());
                        }
                    };
                }
                final long min = minTime.get();
                final long max = maxTime.get();
                // get row count in query
                StringBuilder rowCountBuilder = new StringBuilder("SELECT COUNT(*) FROM ").append(tableName);
                StringBuilder statementBuilder = new StringBuilder("SELECT * FROM ").append(tableName);
                if (min == Long.MIN_VALUE && max == Long.MAX_VALUE) {   // NO RESTRICTIONS
                } else if (max == Long.MAX_VALUE) {
                    rowCountBuilder.append(" WHERE time > ").append(min);
                    statementBuilder.append(" WHERE time > ").append(min);
                } else if (min == Long.MIN_VALUE) {
                    rowCountBuilder.append(" WHERE time < ").append(max);
                    statementBuilder.append(" WHERE time < ").append(max);
                } else {
                    rowCountBuilder.append(" WHERE time BETWEEN ").append(min).append(" AND ").append(max);
                    statementBuilder.append(" WHERE time BETWEEN ").append(min).append(" AND ").append(max);
                }
                statementBuilder.append(" ORDER BY time");
                final String rowCountStr = rowCountBuilder.toString();
                final String statementStr = statementBuilder.toString();
                return new Task<List<Transaction>>() {
                    @Override
                    protected List<Transaction> call() throws SQLException {
                        List<Transaction> data = new ArrayList<>();
                        // get row count
                        long rowCount;
                        try (PreparedStatement rowCountStatement = connection.prepareStatement(rowCountStr)) {
                            ResultSet rowCountSet = rowCountStatement.executeQuery();
                            rowCountSet.next();
                            rowCount = rowCountSet.getLong(1);
                        }
                        long workDone = 0;
                        try (PreparedStatement statement = connection.prepareStatement(statementStr)) {
                            // get data
                            ResultSet set = statement.executeQuery();
                            while (set.next()) {
                                data.add(new Transaction(set.getLong(1), set.getLong(2), set.getDouble(3),
                                        set.getDouble(4), set.getInt(5), platform));
                                updateProgress(++workDone, rowCount);
                            }
                        }
                        return data;
                    }
                };
            }

            @Override
            protected void succeeded() {
                calculator.restart();
            }
        }

        public class Calculator extends Service<XYChart.Series<Number, Number>[]> {

            public Calculator() {}

            @Override
            protected Task<XYChart.Series<Number, Number>[]> createTask() {
                final List<Transaction> trades = reader.getValue();
                assert trades != null;
                if (getInterval() < 1) {
                    throw new IllegalStateException("interval must be at least 1");
                }
                return new Task<XYChart.Series<Number, Number>[]>() {
                    @Override
                    protected XYChart.Series<Number, Number>[] call() {
                        final long currentTime = System.currentTimeMillis()/1000;   // for extending to now
                        final List<Transaction> data = getTimeRestrictedTrades(trades);   // restricted between min and max
                        final int dataSize = data.size();
                        final int interval = getInterval();
                        XYChart.Series<Number, Number> priceSeries = new XYChart.Series<>();
                        XYChart.Series<Number, Number> volumeSeries = new XYChart.Series<>();
                        // sort
                        if (!data.isEmpty()) {
                            int workDone = 0;
                            Collections.sort(data);
                            List<Transaction> intervalTrades = new ArrayList<>();
                            double lastPrice = data.get(0).getPrice();
                            long intervalStartTime = calculateStartTime(data.get(0).getTime());
                            for (int i = 0, max = data.size(); i < max; i++) {
                                Transaction item = data.get(i);
                                while (item.getTime() >= intervalStartTime + interval) {    // add previous interval if new interval
                                    if (intervalTrades.isEmpty()) {
                                        priceSeries.getData().add(new XYChart.Data<>(intervalStartTime, lastPrice,
                                                new CandleStickValues(lastPrice, lastPrice, lastPrice)));
                                        volumeSeries.getData().add(new XYChart.Data<>(intervalStartTime, 0, 0.0));
                                    } else {
                                        List<Double> prices = intervalTrades.stream().map(Transaction::getPrice).collect(Collectors.toList());
                                        List<Double> volumes = intervalTrades.stream().map(Transaction::getVolume).collect(Collectors.toList());
                                        List<Double> qVolumes = intervalTrades.stream().map(
                                                transaction -> transaction.getPrice()*transaction.getVolume()).collect(Collectors.toList());
                                        double open = intervalTrades.get(0).getPrice();
                                        double close = intervalTrades.get(intervalTrades.size() - 1).getPrice();
                                        double high = Collections.max(prices);
                                        double low = Collections.min(prices);
                                        priceSeries.getData().add(new XYChart.Data<>(intervalStartTime, open, new CandleStickValues(close, high, low)));
                                        volumeSeries.getData().add(new XYChart.Data<>(intervalStartTime, volumes.stream().mapToDouble(Double::doubleValue).sum(),
                                                qVolumes.stream().mapToDouble(Double::doubleValue).sum()));
                                        intervalTrades.clear();
                                    }
                                    intervalStartTime += interval;
                                }
                                intervalTrades.add(item);
                                lastPrice = item.getPrice();
                                updateProgress(++workDone, dataSize);
                            }
                            do {
                                if (intervalTrades.isEmpty()) {
                                    priceSeries.getData().add(new XYChart.Data<>(intervalStartTime, lastPrice,
                                            new CandleStickValues(lastPrice, lastPrice, lastPrice)));
                                    volumeSeries.getData().add(new XYChart.Data<>(intervalStartTime, 0, 0.0));
                                } else {
                                    List<Double> prices = intervalTrades.stream().map(Transaction::getPrice).collect(Collectors.toList());
                                    List<Double> volumes = intervalTrades.stream().map(Transaction::getVolume).collect(Collectors.toList());
                                    List<Double> qVolumes = intervalTrades.stream().map(
                                            transaction -> transaction.getPrice()*transaction.getVolume()).collect(Collectors.toList());
                                    double open = intervalTrades.get(0).getPrice();
                                    double close = intervalTrades.get(intervalTrades.size() - 1).getPrice();
                                    double high = Collections.max(prices);
                                    double low = Collections.min(prices);
                                    priceSeries.getData().add(new XYChart.Data<>(intervalStartTime, open, new CandleStickValues(close, high, low)));
                                    volumeSeries.getData().add(new XYChart.Data<>(intervalStartTime, volumes.stream().mapToDouble(Double::doubleValue).sum(),
                                            qVolumes.stream().mapToDouble(Double::doubleValue).sum()));
                                    intervalTrades.clear();
                                }
                                intervalStartTime += interval;
                            } while (currentTime >= intervalStartTime + interval);
                        }
                        return new XYChart.Series[] {priceSeries, volumeSeries};
                    }
                };
            }

            /**
             * Get all the trades within the min and max times.
             * @param trades trades, ordered by time ascending
             * @return all trades within min and max times
             */
            private List<Transaction> getTimeRestrictedTrades(List<Transaction> trades) {
                final long minTime = getMinTime(), maxTime = getMaxTime();
                int minIndex = 0, maxIndex = trades.size();
                for (int i = 0, max = trades.size(); i < max; i++) {
                    if (trades.get(i).getTime() >= minTime) {
                        minIndex = i;
                        break;
                    }
                }
                for (int i = trades.size() - 1; i >= 0; i--) {
                    if (trades.get(i).getTime() <= maxTime) {
                         maxIndex = i + 1;
                         break;
                    }
                }
                return new ArrayList<>(trades.subList(minIndex, maxIndex));
            }

            private long calculateStartTime(long minTime) {
                final int interval = getInterval();
                if (interval < 3600) {  // minute
                    return Utils.snapToPreviousMinute(minTime, interval/60);
                }
                if (interval < 86400) { // hour
                    return Utils.snapToPreviousHour(minTime, interval/3600);
                }
                if (interval < 86400*7) {   // day
                    return Utils.snapToPreviousDay(minTime, interval/86400);
                }
                return Utils.snapToPreviousWeek(minTime, interval/(86400*7), DayOfWeek.MONDAY);
            }
        }
    }
}
