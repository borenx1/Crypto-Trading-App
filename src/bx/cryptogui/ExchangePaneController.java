package bx.cryptogui;

import bx.cryptogui.data.*;
import bx.cryptogui.exchangeapi.AuthorisationInvalidException;
import bx.cryptogui.exchangeapi.ExchangeAPI;
import bx.cryptogui.exchangeapi.HTTPException;
import bx.cryptogui.manager.Account;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;


public class ExchangePaneController implements Initializable {

    private ObjectProperty<Account> account = new SimpleObjectProperty<Account>() {
        @Override
        protected void invalidated() {
            bindAccountControls(getAccount());
        }
    };
    public final Account getAccount() {
        return account.get();
    }
    public final void setAccount(Account account) {
        this.account.set(account);
    }
    public final ObjectProperty<Account> accountProperty() {
        return account;
    }

    private ObjectProperty<ExchangeAPI> exchangeAPI = new SimpleObjectProperty<>();
    public final ExchangeAPI getExchangeAPI() {
        return exchangeAPI.get();
    }
    public final void setExchangeAPI(ExchangeAPI api) {
        exchangeAPI.set(api);
    }
    public final ObjectProperty<ExchangeAPI> exchangeAPIProperty() {
        return exchangeAPI;
    }

    private ReadOnlyObjectWrapper<CurrencyPair> currencyPair = new ReadOnlyObjectWrapper<>(this, "currencyPair");
    public final CurrencyPair getCurrencyPair() {
        return currencyPair.get();
    }
    public final ReadOnlyObjectProperty<CurrencyPair> currencyPairProperty() {
        return currencyPair.getReadOnlyProperty();
    }

    private final ReadOnlyLongWrapper lastUpdated = new ReadOnlyLongWrapper(0);
    public final long getLastUpdated() {
        return lastUpdated.get();
    }
    public final ReadOnlyLongProperty lastUpdatedProperty() {
        return lastUpdated.getReadOnlyProperty();
    }

    private Timeline lastUpdatedTimeline;
    private final AccountAPIService accountAPIService = new AccountAPIService();

    private final BooleanProperty allServicesDone = new SimpleBooleanProperty(true);
    protected final boolean isAllServicesDone() {
        return allServicesDone.get();
    }
    protected final BooleanProperty allServicesDoneProperty() {
        return allServicesDone;
    }
    {
        allServicesDone.bind(Bindings.createBooleanBinding(() -> {
            for (Service service: new Service[] {accountAPIService}) {
                switch (service.getState()) {
                    case RUNNING:
                    case SCHEDULED:
                        return false;
                }
            }
            return true;
        }, accountAPIService.stateProperty()));
    }

    @FXML protected ChoiceBox<CurrencyPair> currencyChoice;
    @FXML protected Label lastUpdatedText;
    @FXML protected Button updateButton;

    @FXML protected Label accountText;
    @FXML protected Button loadAccountButton;
    @FXML protected Label btcBalanceText;
    @FXML protected Label ltcBalanceText;
    @FXML protected Label usdBalanceText;
    @FXML protected Label nzdBalanceText;
    @FXML protected Label accountUpdatedText;
    @FXML protected ProgressIndicator accountUpdateProgress;
    @FXML protected Button accountUpdateButton;

    @FXML protected RadioButton buyToggle;
    @FXML protected RadioButton sellToggle;
    @FXML protected TextField tradeRateField;
    @FXML protected TextField tradeAmountField;
    @FXML protected Button tradeAmountMaxButton;
    @FXML protected Button placeOrderButton;
    @FXML protected TableView<Order> openOrdersTable;
    @FXML protected TableView<Transaction> pastTradesTable;
    @FXML private TextArea messageView;

    @FXML private GridPane chartPane;
    @FXML private ChartPaneController chartPaneController;
    protected ChartPaneController chartCtrl;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chartCtrl = chartPaneController;
        currencyPair.bind(currencyChoice.getSelectionModel().selectedItemProperty());
        lastUpdatedTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> lastUpdated.set(lastUpdated.get()+1)));

        currencyChoice.setConverter(new StringConverter<CurrencyPair>() {
            @Override
            public String toString(CurrencyPair object) {
                return object.getBaseCurrency() + "/" + object.getQuoteCurrency();
            }
            @Override
            public CurrencyPair fromString(String string) {
                int slashIndex = string.indexOf('/');
                Currency cur1 = Currency.valueOf(string.substring(0, slashIndex));
                Currency cur2 = Currency.valueOf(string.substring(slashIndex+1));
                return new CurrencyPair(cur1, cur2);
            }
        });
        lastUpdatedText.textProperty().bind(lastUpdated.asString("%ss ago"));
        lastUpdatedTimeline.setCycleCount(Timeline.INDEFINITE);
        lastUpdatedTimeline.play();

        accountUpdateProgress.visibleProperty().bind(allServicesDoneProperty().not());
        accountUpdateButton.setOnAction(event -> {
            if (getAccount() == null) {
                logMessage("Update account failed: No account loaded");
            } else if (getAccount().isAuthInvalid()) {
                logMessage("Update account failed: Invalid account");
            } else {
                updateAccount();
            }
        });
    }

    public void resetLastUpdated() {
        lastUpdatedTimeline.stop();
        lastUpdated.set(0);
        lastUpdatedTimeline.playFromStart();
    }

    public void updateAccount() {
        assert getAccount() != null;
        if (accountAPIService.isRunning()) {
            accountAPIService.cancel();
        }
        accountAPIService.setAccount(getAccount());
        accountAPIService.reset();
        accountAPIService.start();
    }

    public void logMessage(String message) {
        final String newLine = Utils.formatTime(System.currentTimeMillis()/1000) + "  " + message;
        final String oldText = messageView.getText();
        String newText = oldText + (oldText.endsWith("\n") || oldText.isEmpty() ? "" : "\n") + newLine;
        messageView.setText(newText);
    }

    private void bindAccountControls(final Account acc) {
        resetAccountControls();
        if (acc != null) {
            if (exchangeAPI.get() != null) {
                exchangeAPI.get().setAuthorisation(acc.getAuthorisation());
            }
            accountText.textProperty().bind(Bindings.createStringBinding(() -> {
                if (acc.isAuthInvalid()) {
                    return "Authorisation Invalid";
                } else {
                    return acc.getName().isEmpty() ? "Yes" : acc.getName();
                }
            }, acc.authInvalidProperty()));
            final DecimalFormat format = new DecimalFormat("###,###.##########");
            btcBalanceText.textProperty().bind(Bindings.createStringBinding(() -> {
                if (acc.getSupportedCurrencies().contains(Currency.BTC)) {
                    return format.format(acc.getBalance().get(Currency.BTC));
                } else {
                    return "Unavailable";
                }
            }, acc.getBalance()));
            ltcBalanceText.textProperty().bind(Bindings.createStringBinding(() -> {
                if (acc.getSupportedCurrencies().contains(Currency.LTC)) {
                    return format.format(acc.getBalance().get(Currency.LTC));
                } else {
                    return "Unavailable";
                }
            }, acc.getBalance()));
            usdBalanceText.textProperty().bind(Bindings.createStringBinding(() -> {
                if (acc.getSupportedCurrencies().contains(Currency.USD)) {
                    return format.format(acc.getBalance().get(Currency.USD));
                } else {
                    return "Unavailable";
                }
            }, acc.getBalance()));
            nzdBalanceText.textProperty().bind(Bindings.createStringBinding(() -> {
                if (acc.getSupportedCurrencies().contains(Currency.NZD)) {
                    return format.format(acc.getBalance().get(Currency.NZD));
                } else {
                    return "Unavailable";
                }
            }, acc.getBalance()));
            accountUpdatedText.textProperty().bind(Bindings.createStringBinding(() -> {
                if (acc.getLastUpdated() <= 0) {
                    return "Never";
                } else {
                    return Utils.formatDateTime(acc.getLastUpdated());
                }
            }, acc.lastUpdatedProperty()));
            openOrdersTable.itemsProperty().bind(acc.openOrdersProperty());
            openOrdersTable.scrollTo(0);
            pastTradesTable.itemsProperty().bind(acc.pastTradesProperty());
            pastTradesTable.scrollTo(0);
            updateAccount();    // update and end
        }
    }

    private void resetAccountControls() {
        if (exchangeAPI.get() != null) {
            exchangeAPI.get().setAuthorisation(null);
        }
        accountText.textProperty().unbind();
        accountText.setText("None");
        btcBalanceText.textProperty().unbind();
        btcBalanceText.setText("0");
        ltcBalanceText.textProperty().unbind();
        ltcBalanceText.setText("0");
        usdBalanceText.textProperty().unbind();
        usdBalanceText.setText("0");
        nzdBalanceText.textProperty().unbind();
        nzdBalanceText.setText("0");
        accountUpdatedText.textProperty().unbind();
        accountUpdatedText.setText("");
        openOrdersTable.itemsProperty().unbind();
        openOrdersTable.getItems().clear();
        pastTradesTable.itemsProperty().unbind();
        pastTradesTable.getItems().clear();
    }

    // --------------------------- CHART CONTROLLER --------------------------------------------------------------------

    public void updateData(List<Transaction> data, List<XYChart.Data<Number, Number>> prices,
                           List<XYChart.Data<Number, Number>> volumes, int interval) {
        chartCtrl.updateData(data, prices, volumes, interval);
    }

    public void refreshTimeAxis() {
        chartCtrl.refreshTimeAxis();
    }

    public void setPriceAxisRange(double lower, double upper, double tickUnit) {
        chartCtrl.setPriceAxisRange(lower, upper, tickUnit);
    }

    public class AccountAPIService extends Service<Object> {

        private ObjectProperty<Account> account = new SimpleObjectProperty<>();
        public final Account getAccount() {
            return account.get();
        }
        public final void setAccount(Account account) {
            this.account.set(account);
        }
        public final ObjectProperty<Account> accountProperty() {
            return account;
        }

        @Override
        protected Task<Object> createTask() {
            if (getAccount() == null) {
                return new Task<Object>() {
                    @Override
                    protected Object call() throws IllegalStateException {
                        throw new IllegalStateException("Account is null");
                    }
                };
            }
            assert getExchangeAPI() != null : "No exchangeAPI";
            return new Task<Object>() {
                @Override
                protected Object call() throws Exception {
                    final Account account = getAccount();
                    final ExchangeAPI api = getExchangeAPI();
                    // balance
                    Map<Currency, Double> balance = api.getBalance();
                    // open orders
                    List<Order> openOrders = api.getOpenOrders();
                    // trade history
                    List<Transaction> tradeHistory = api.getTradeHistory();
                    return new Object[] {balance, openOrders, tradeHistory};
                }
            };
        }

        @Override
        protected void succeeded() {
            getAccount().setAuthInvalid(false);
            getAccount().setLastUpdated(System.currentTimeMillis()/1000);
            Object[] value = (Object[]) getValue();
            Map<Currency, Double> balance = (Map<Currency, Double>) value[0];
            List<Order> openOrders = (List<Order>) value[1];
            List<Transaction> tradeHistory = (List<Transaction>) value[2];
            for (Currency cur: balance.keySet()) {
                if (getAccount().supportsCurrency(cur)) {
                    getAccount().getBalance().put(cur, balance.get(cur));
                }
            }
            getAccount().getOpenOrders().setAll(openOrders);
            getAccount().getPastTrades().setAll(tradeHistory);

            logMessage("Account updated successfully");
        }

        @Override
        protected void failed() {
            Throwable exception = getException();
            if (exception instanceof IllegalStateException) {
                logMessage("Update account failed: No account loaded");
            } else if (exception instanceof AuthorisationInvalidException) {
                getAccount().setAuthInvalid(true);
                logMessage("Update account failed: Invalid account");
            } else {
                logMessage(exception.getMessage());
            }
        }
    }
}
