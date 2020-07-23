package bx.cryptogui;

import bx.cryptogui.manager.APIManager;
import bx.cryptogui.manager.DatabaseManager;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import bx.cryptogui.data.*;

/**
 * <h3>Application Components:</h3>
 * 1. Database updater:
 * <p>Periodically updates the database from the exchange APIs</p>
 * 2. Account updaters:
 * <p>Updates account information on request</p>
 * 3. Graph updaters:
 * <p>
 *     Updates the graphs on request, can set refresh rates. This
 *     includes order book info, ticker info.
 * </p>
 *
 */
public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Columns: time INT, price REAL, volume REAL, type INT (0-buy, 1-sell, other-unknown).
     */
    private Connection conn;
    private MainController controller;

    private DatabaseManager databaseManager;
    private final APIManager apiManager = new APIManager();

    @Override
    public void init() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:cryptoExchanges.db");
        databaseManager = new DatabaseManager(conn);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Crypto Market Watch");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/bx/cryptogui/main.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        setUpListenersAndProperties();

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(800);
        primaryStage.show();

        controller.logMessage("Welcome");
        requestDatabaseUpdate();
    }



    private void setUpListenersAndProperties() {
        setUpDatabaseManager();
        setUpAPIManager();
    }

    private void setUpDatabaseManager() {
        final DatabaseManager.ExchangeManager wexManager = databaseManager.getExchangeManager(Exchange.WEX);
        final DatabaseManager.ExchangeManager bitstampManager = databaseManager.getExchangeManager(Exchange.BITSTAMP);
        final DatabaseManager.ExchangeManager coinbaseManager = databaseManager.getExchangeManager(Exchange.COINBASE);
        final DatabaseManager.ExchangeManager cryptopiaManager = databaseManager.getExchangeManager(Exchange.CRYPTOPIA);
        final DatabaseManager.ExchangeManager kiwicoinManager = databaseManager.getExchangeManager(Exchange.KIWICOIN);
        // WRITER
        final SettingsController settingsCtrl = controller.settingsCtrl;  // More concise
        // Control display
        settingsCtrl.wexUpdatePane.timeProperty().bind(wexManager.lastUpdatedProperty());
        settingsCtrl.wexUpdatePane.newRowsProperty().bind(wexManager.newRowsProperty());
        settingsCtrl.wexUpdatePane.progressVisibleProperty().bind(wexManager.writingProperty());

        settingsCtrl.bitstampUpdatePane.timeProperty().bind(bitstampManager.lastUpdatedProperty());
        settingsCtrl.bitstampUpdatePane.newRowsProperty().bind(bitstampManager.newRowsProperty());
        settingsCtrl.bitstampUpdatePane.progressVisibleProperty().bind(bitstampManager.writingProperty());

        settingsCtrl.coinbaseUpdatePane.timeProperty().bind(coinbaseManager.lastUpdatedProperty());
        settingsCtrl.coinbaseUpdatePane.newRowsProperty().bind(coinbaseManager.newRowsProperty());
        settingsCtrl.coinbaseUpdatePane.progressVisibleProperty().bind(coinbaseManager.writingProperty());

        settingsCtrl.cryptopiaUpdatePane.timeProperty().bind(cryptopiaManager.lastUpdatedProperty());
        settingsCtrl.cryptopiaUpdatePane.newRowsProperty().bind(cryptopiaManager.newRowsProperty());
        settingsCtrl.cryptopiaUpdatePane.progressVisibleProperty().bind(cryptopiaManager.writingProperty());

        settingsCtrl.kiwicoinUpdatePane.timeProperty().bind(kiwicoinManager.lastUpdatedProperty());
        settingsCtrl.kiwicoinUpdatePane.newRowsProperty().bind(kiwicoinManager.newRowsProperty());
        settingsCtrl.kiwicoinUpdatePane.progressVisibleProperty().bind(kiwicoinManager.writingProperty());
        // Button action
        settingsCtrl.updateDatabaseButton.setOnAction(event -> requestDatabaseUpdate());
        // Auto update
        settingsCtrl.lastUpdatedProperty().addListener(observable -> {
            if (settingsCtrl.getLastUpdated() >= settingsCtrl.getAutoUpdateDatabaseRate()) {
                requestDatabaseUpdate();
            }
        });
        // Writer failed log message (instead of passing silently)
        for (Exchange exchange: databaseManager.getSupportedExchanges()) {
            DatabaseManager.ExchangeManager manager = databaseManager.getExchangeManager(exchange);
            manager.setOnWritingFailed(event -> {
                Throwable exception = event.getSource().getException();
                controller.logMessage("Failed: " + exception, "Database Writer - " + exchange);
                exception.printStackTrace();
            });
            manager.setOnReadingFailed(event -> {
                Throwable exception = event.getSource().getException();
                controller.logMessage("Failed: " + exception, "Database Reader - " + exchange);
                exception.printStackTrace();
            });
            manager.setOnCalculatingFailed(event -> {
                Throwable exception = event.getSource().getException();
                controller.logMessage("Failed: " + exception, "Database Calculator - " + exchange);
                exception.printStackTrace();
            });
        }
        databaseManager.getExchangeManager(Exchange.WEX).newRowsProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() >= 5000) {
                controller.logMessage("New rows: " + newValue, Exchange.WEX.toString());
            }
        });
        databaseManager.getExchangeManager(Exchange.COINBASE).newRowsProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() >= 5000) {
                controller.logMessage("New rows: " + newValue, Exchange.COINBASE.toString());
            }
        });
        databaseManager.getExchangeManager(Exchange.CRYPTOPIA).newRowsProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() >= 2000) {
                controller.logMessage("New rows: " + newValue, Exchange.CRYPTOPIA.toString());
            }
        });
        // READER
        // Time interval
        wexManager.intervalProperty().bind(Bindings.createIntegerBinding(() -> {
            return (int) controller.wexCtrl.chartCtrl.timeIntervalChoice.getValue().getSeconds();
        }, controller.wexCtrl.chartCtrl.timeIntervalChoice.valueProperty()));
        bitstampManager.intervalProperty().bind(Bindings.createIntegerBinding(() -> {
            return (int) controller.bitstampCtrl.chartCtrl.timeIntervalChoice.getValue().getSeconds();
        }, controller.bitstampCtrl.chartCtrl.timeIntervalChoice.valueProperty()));
        coinbaseManager.intervalProperty().bind(Bindings.createIntegerBinding(() -> {
            return (int) controller.coinbaseCtrl.chartCtrl.timeIntervalChoice.getValue().getSeconds();
        }, controller.coinbaseCtrl.chartCtrl.timeIntervalChoice.valueProperty()));
        cryptopiaManager.intervalProperty().bind(Bindings.createIntegerBinding(() -> {
            return (int) controller.cryptopiaCtrl.chartCtrl.timeIntervalChoice.getValue().getSeconds();
        }, controller.cryptopiaCtrl.chartCtrl.timeIntervalChoice.valueProperty()));
        kiwicoinManager.intervalProperty().bind(Bindings.createIntegerBinding(() -> {
            return (int) controller.kiwicoinCtrl.chartCtrl.timeIntervalChoice.getValue().getSeconds();
        }, controller.kiwicoinCtrl.chartCtrl.timeIntervalChoice.valueProperty()));
        // Time interval action, Update graph button
        for (Exchange exchange: databaseManager.getSupportedExchanges()) {
            final ExchangePaneController ctrl = controller.getController(exchange);
            ctrl.updateButton.setOnAction(event -> {
                plotFromDatabase(new TradePlatform(exchange, ctrl.getCurrencyPair()));
                updateTickerAndOrders(new TradePlatform(exchange, ctrl.getCurrencyPair()));
            });
            ctrl.chartCtrl.timeIntervalChoice.valueProperty().addListener(observable -> {
                calculateThenPlot(new TradePlatform(exchange, ctrl.getCurrencyPair()));
                updateTickerAndOrders(new TradePlatform(exchange, ctrl.getCurrencyPair()));
            });
            ctrl.currencyChoice.getSelectionModel().selectedItemProperty().addListener(observable -> {
                calculateThenPlot(new TradePlatform(exchange, ctrl.getCurrencyPair()));
                updateTickerAndOrders(new TradePlatform(exchange, ctrl.getCurrencyPair()));
            });
        }
        // Loading Pane
        controller.wexCtrl.chartCtrl.chartLoadingProgress.progressProperty().bind(Bindings.createDoubleBinding(() -> {
            if (wexManager.isReading()) {
                return wexManager.getReadingProgress();
            } else {
                return wexManager.getCalculatingProgress();
            }
        }, wexManager.readingProperty(), wexManager.readingProgressProperty(), wexManager.calculatingProgressProperty()));
        controller.wexCtrl.chartCtrl.chartLoadingText.textProperty().bind(Bindings.createStringBinding(() -> {
            return wexManager.isReading() ? "Reading from database..." : "Calculating...";
        }, wexManager.readingProperty()));
        controller.wexCtrl.chartCtrl.chartLoadingPane.visibleProperty().bind(wexManager.calculatingProperty().or(wexManager.readingProperty()));

        controller.bitstampCtrl.chartCtrl.chartLoadingProgress.progressProperty().bind(Bindings.createDoubleBinding(() -> {
            if (bitstampManager.isReading()) {
                return bitstampManager.getReadingProgress();
            } else {
                return bitstampManager.getCalculatingProgress();
            }
        }, bitstampManager.readingProperty(), bitstampManager.readingProgressProperty(), bitstampManager.calculatingProgressProperty()));
        controller.bitstampCtrl.chartCtrl.chartLoadingText.textProperty().bind(Bindings.createStringBinding(() -> {
            return bitstampManager.isReading() ? "Reading from database..." : "Calculating...";
        }, bitstampManager.readingProperty()));
        controller.bitstampCtrl.chartCtrl.chartLoadingPane.visibleProperty().bind(bitstampManager.calculatingProperty().or(bitstampManager.readingProperty()));

        controller.coinbaseCtrl.chartCtrl.chartLoadingProgress.progressProperty().bind(Bindings.createDoubleBinding(() -> {
            if (coinbaseManager.isReading()) {
                return coinbaseManager.getReadingProgress();
            } else {
                return coinbaseManager.getCalculatingProgress();
            }
        }, coinbaseManager.readingProperty(), coinbaseManager.readingProgressProperty(), coinbaseManager.calculatingProgressProperty()));
        controller.coinbaseCtrl.chartCtrl.chartLoadingText.textProperty().bind(Bindings.createStringBinding(() -> {
            return coinbaseManager.isReading() ? "Reading from database..." : "Calculating...";
        }, coinbaseManager.readingProperty()));
        controller.coinbaseCtrl.chartCtrl.chartLoadingPane.visibleProperty().bind(coinbaseManager.calculatingProperty().or(coinbaseManager.readingProperty()));

        controller.cryptopiaCtrl.chartCtrl.chartLoadingProgress.progressProperty().bind(Bindings.createDoubleBinding(() -> {
            if (cryptopiaManager.isReading()) {
                return cryptopiaManager.getReadingProgress();
            } else {
                return cryptopiaManager.getCalculatingProgress();
            }
        }, cryptopiaManager.readingProperty(), cryptopiaManager.readingProgressProperty(), cryptopiaManager.calculatingProgressProperty()));
        controller.cryptopiaCtrl.chartCtrl.chartLoadingText.textProperty().bind(Bindings.createStringBinding(() -> {
            return cryptopiaManager.isReading() ? "Reading from database..." : "Calculating...";
        }, cryptopiaManager.readingProperty()));
        controller.cryptopiaCtrl.chartCtrl.chartLoadingPane.visibleProperty().bind(cryptopiaManager.calculatingProperty().or(cryptopiaManager.readingProperty()));

        // Special cases
        controller.kiwicoinCtrl.chartCtrl.chartLoadingProgress.setProgress(-1);
        controller.kiwicoinCtrl.chartCtrl.chartLoadingText.setText("No past trades available");
        controller.kiwicoinCtrl.chartCtrl.chartLoadingPane.setVisible(true);

        controller.kiwicoinCtrl.updateButton.setOnAction(event -> apiManager.startRetrieving(TradePlatform.KIWICOIN_BTC_NZD));
    }

    private void setUpAPIManager() {
        // ticker
        final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("MMM dd HH:mm:ss");
        for (Exchange exchange: apiManager.getSupportedExchanges()) {
            final ExchangePaneController ctrl = controller.getController(exchange);
            final APIManager.APIRetriever retriever = apiManager.getAPIRetriever(exchange);
            ctrl.chartCtrl.tickerPrice.textProperty().bind(Bindings.createStringBinding(() -> {
                Ticker ticker = retriever.getTicker();
                return ticker == null ? null : Utils.formatDecimal(ticker.getLast(), 10, false);
            }, retriever.tickerProperty()));
            ctrl.chartCtrl.tickerPrice.styleProperty().bind(Bindings.createStringBinding(() -> {
                Ticker ticker = retriever.getTicker();
                if (ticker == null) {
                    return null;
                }
                switch (ticker.getType()) {
                    case 0:
                        return "-fx-fill: -price-rise;";
                    case 1:
                        return "-fx-fill: -price-fall;";
                    default:
                        return null;
                }
            }, retriever.tickerProperty()));
            ctrl.chartCtrl.tickerTime.textProperty().bind(Bindings.createStringBinding(() -> {
                Ticker ticker = retriever.getTicker();
                return ticker == null ? null : timeFormatter.format(LocalDateTime.ofEpochSecond(ticker.getTime(), 0, Utils.getLocalZoneOffset()));
            }, retriever.tickerProperty()));
            ctrl.chartCtrl.tickerVolume.textProperty().bind(Bindings.createStringBinding(() -> {
                Ticker ticker = retriever.getTicker();
                return ticker == null ? null : Utils.formatDecimal(ticker.getVolume(), 8, true);
            }, retriever.tickerProperty()));
            // Order book
            ctrl.chartCtrl.buyOrders.itemsProperty().bind(Bindings.createObjectBinding(() -> {
                List<Order> orders = retriever.getBidOrders();
                return orders == null ? FXCollections.observableArrayList() : FXCollections.observableArrayList(orders);
            }, retriever.bidOrdersProperty()));
            ctrl.chartCtrl.sellOrders.itemsProperty().bind(Bindings.createObjectBinding(() -> {
                List<Order> orders = retriever.getAskOrders();
                return orders == null ? FXCollections.observableArrayList() : FXCollections.observableArrayList(orders);
            }, retriever.askOrdersProperty()));
        }

        // Exception warning
        for (Exchange exchange: apiManager.getSupportedExchanges()) {
            final APIManager.APIRetriever retriever = apiManager.getAPIRetriever(exchange);
            retriever.setOnFailed(event -> {
                Throwable exception = event.getSource().getException();
                controller.logMessage("Failed: " + exception, "API Retriever - " + exchange);
                exception.printStackTrace();
            });
        }
    }

    public void requestDatabaseUpdate() {
        databaseManager.requestDatabaseUpdate();
        controller.settingsCtrl.resetLastUpdated();
        controller.logMessage("Request update", "Database");
    }

    private void plotFromDatabase(TradePlatform platform) {
        final ExchangePaneController ctrl = controller.getController(platform.getExchange());
        final int maxBars = controller.settingsCtrl.getMaxBarCount();
        final int interval = (int) ctrl.chartCtrl.timeIntervalChoice.getValue().getSeconds();
        final long minTime = System.currentTimeMillis()/1000 - maxBars*interval;
        final DatabaseManager.ExchangeManager manager = databaseManager.getExchangeManager(platform.getExchange());
        // Set properties
        manager.setMinTime(minTime);
        manager.setMaxTime(Long.MAX_VALUE);
        // Set on succeeded
        manager.setOnReadingSucceeded(event -> ctrl.resetLastUpdated());
        manager.setOnCalculatingSucceeded(event -> {
            XYChart.Series<Number, Number>[] series = manager.getCalculatorValue();
            ctrl.updateData(manager.getReaderValue(), series[0].getData(), series[1].getData(), interval);
        });
        // Start service
        databaseManager.startReadingThenCalculate(platform);
    }

    private void calculateThenPlot(TradePlatform platform) {
        final ExchangePaneController ctrl = controller.getController(platform.getExchange());
        final int maxBars = controller.settingsCtrl.getMaxBarCount();
        final int interval = (int) ctrl.chartCtrl.timeIntervalChoice.getValue().getSeconds();
        final long minTime = System.currentTimeMillis()/1000 - maxBars*interval;
        DatabaseManager.ExchangeManager manager = databaseManager.getExchangeManager(platform.getExchange());
        // Set properties
        manager.setMinTime(minTime);
        manager.setMaxTime(Long.MAX_VALUE);
        // Set on succeeded
        manager.setOnCalculatingSucceeded(event -> {
            XYChart.Series<Number, Number>[] series = manager.getCalculatorValue();
            ctrl.updateData(manager.getReaderValue(), series[0].getData(), series[1].getData(), interval);
        });
        // Start service
        databaseManager.startCalculating(platform);
    }

    private void updateTickerAndOrders(TradePlatform platform) {
        apiManager.startRetrieving(platform);
    }
}
