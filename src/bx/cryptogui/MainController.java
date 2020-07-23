package bx.cryptogui;

import bx.cryptogui.data.*;
import bx.cryptogui.exchangeapi.Authorisation;
import bx.cryptogui.exchangeapi.WexAPI;
import bx.cryptogui.manager.Account;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;


public class MainController implements Initializable {

    @FXML protected TabPane exchangeTabPane;

    @FXML protected Tab wexTab;
    @FXML private GridPane wexExchange;
    @FXML private ExchangePaneController wexExchangeController;
    protected ExchangePaneController wexCtrl;

    @FXML protected Tab bitstampTab;
    @FXML private GridPane bitstampExchange;
    @FXML private ExchangePaneController bitstampExchangeController;
    protected ExchangePaneController bitstampCtrl;

    @FXML protected Tab coinbaseTab;
    @FXML private GridPane coinbaseExchange;
    @FXML private ExchangePaneController coinbaseExchangeController;
    protected ExchangePaneController coinbaseCtrl;

    @FXML protected Tab cryptopiaTab;
    @FXML private GridPane cryptopiaExchange;
    @FXML private ExchangePaneController cryptopiaExchangeController;
    protected ExchangePaneController cryptopiaCtrl;

    @FXML protected Tab kiwicoinTab;
    @FXML private GridPane kiwicoinExchange;
    @FXML private ExchangePaneController kiwicoinExchangeController;
    protected ExchangePaneController kiwicoinCtrl;

    @FXML private VBox settings;
    @FXML private SettingsController settingsController;
    protected SettingsController settingsCtrl;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        wexCtrl = wexExchangeController;
        bitstampCtrl = bitstampExchangeController;
        coinbaseCtrl = coinbaseExchangeController;
        cryptopiaCtrl = cryptopiaExchangeController;
        kiwicoinCtrl = kiwicoinExchangeController;
        settingsCtrl = settingsController;
        // customise account pane
        initExchanges();
    }

    private void initExchanges() {
        // load account button
        wexCtrl.loadAccountButton.setOnAction(event -> {
            Optional<Authorisation> result = loadAccountDialog().showAndWait();
            result.ifPresent(authorisation -> {
                wexCtrl.setAccount(new Account(authorisation, Exchange.WEX));
                wexCtrl.getAccount().addSupportedCurrency(Currency.BTC, Currency.USD, Currency.LTC);
            });
        });
        bitstampCtrl.loadAccountButton.setOnAction(event -> {
            Optional<Authorisation> result = loadAccountDialog().showAndWait();
            result.ifPresent(authorisation -> {
                bitstampCtrl.setAccount(new Account(authorisation, Exchange.BITSTAMP));
                bitstampCtrl.getAccount().addSupportedCurrency(Currency.BTC, Currency.USD, Currency.LTC);
            });
        });
        coinbaseCtrl.loadAccountButton.setOnAction(event -> {
            Optional<Authorisation> result = loadAccountDialog().showAndWait();
            result.ifPresent(authorisation -> {
                coinbaseCtrl.setAccount(new Account(authorisation, Exchange.COINBASE));
                coinbaseCtrl.getAccount().addSupportedCurrency(Currency.BTC, Currency.USD, Currency.LTC);
            });
        });
        // TODO cryptopia
        kiwicoinCtrl.loadAccountButton.setOnAction(event -> {
            Optional<Authorisation> result = loadAccountDialog().showAndWait();
            result.ifPresent(authorisation -> {
                kiwicoinCtrl.setAccount(new Account(authorisation, Exchange.KIWICOIN));
                kiwicoinCtrl.getAccount().addSupportedCurrency(Currency.BTC, Currency.NZD);
            });
        });
        // set exchange API
        wexCtrl.setExchangeAPI(new WexAPI());


        // currency pair choicebox
        wexCtrl.currencyChoice.getItems().addAll(CurrencyPair.BTC_USD, CurrencyPair.LTC_USD, CurrencyPair.ETH_USD);
        bitstampCtrl.currencyChoice.getItems().addAll(CurrencyPair.BTC_USD, CurrencyPair.LTC_USD, CurrencyPair.ETH_USD);
        coinbaseCtrl.currencyChoice.getItems().addAll(CurrencyPair.BTC_USD, CurrencyPair.LTC_USD, CurrencyPair.ETH_USD);
        cryptopiaCtrl.currencyChoice.getItems().addAll(CurrencyPair.BTC_USD, CurrencyPair.BTC_NZD,
                CurrencyPair.LTC_USD, CurrencyPair.LTC_NZD, CurrencyPair.ETH_USD, CurrencyPair.ETH_NZD);
        kiwicoinCtrl.currencyChoice.getItems().addAll(CurrencyPair.BTC_NZD);
        wexCtrl.currencyChoice.getSelectionModel().select(0);
        bitstampCtrl.currencyChoice.getSelectionModel().select(0);
        coinbaseCtrl.currencyChoice.getSelectionModel().select(0);
        cryptopiaCtrl.currencyChoice.getSelectionModel().select(0);
        kiwicoinCtrl.currencyChoice.getSelectionModel().select(0);
        // trade platform text (on chart pane)
        wexCtrl.chartCtrl.tradePlatformText.textProperty().bind(Bindings.createStringBinding(() -> {
            CurrencyPair pair = wexCtrl.currencyChoice.getSelectionModel().getSelectedItem();
            return String.format("WEX %s/%s", pair.getBaseCurrency(), pair.getQuoteCurrency());
        }, wexCtrl.currencyChoice.getSelectionModel().selectedItemProperty()));
        bitstampCtrl.chartCtrl.tradePlatformText.textProperty().bind(Bindings.createStringBinding(() -> {
            CurrencyPair pair = bitstampCtrl.currencyChoice.getSelectionModel().getSelectedItem();
            return String.format("Bitstamp %s/%s", pair.getBaseCurrency(), pair.getQuoteCurrency());
        }, bitstampCtrl.currencyChoice.getSelectionModel().selectedItemProperty()));
        coinbaseCtrl.chartCtrl.tradePlatformText.textProperty().bind(Bindings.createStringBinding(() -> {
            CurrencyPair pair = coinbaseCtrl.currencyChoice.getSelectionModel().getSelectedItem();
            return String.format("Coinbase %s/%s", pair.getBaseCurrency(), pair.getQuoteCurrency());
        }, coinbaseCtrl.currencyChoice.getSelectionModel().selectedItemProperty()));
        cryptopiaCtrl.chartCtrl.tradePlatformText.textProperty().bind(Bindings.createStringBinding(() -> {
            CurrencyPair pair = cryptopiaCtrl.currencyChoice.getSelectionModel().getSelectedItem();
            return String.format("Cryptopia %s/%s", pair.getBaseCurrency(), pair.getQuoteCurrency());
        }, cryptopiaCtrl.currencyChoice.getSelectionModel().selectedItemProperty()));
        kiwicoinCtrl.chartCtrl.tradePlatformText.textProperty().bind(Bindings.createStringBinding(() -> {
            CurrencyPair pair = kiwicoinCtrl.currencyChoice.getSelectionModel().getSelectedItem();
            return String.format("Kiwi-Coin %s/%s", pair.getBaseCurrency(), pair.getQuoteCurrency());
        }, kiwicoinCtrl.currencyChoice.getSelectionModel().selectedItemProperty()));

        // Disable certain exchange controls
        kiwicoinCtrl.chartCtrl.timeIntervalChoice.setDisable(true);
    }

    public Exchange getSelectedExchange() {
        Tab selected = exchangeTabPane.getSelectionModel().getSelectedItem();
        if (selected == wexTab) {
            return Exchange.WEX;
        } else if (selected == bitstampTab) {
            return Exchange.BITSTAMP;
        } else if (selected == coinbaseTab) {
            return Exchange.COINBASE;
        } else if (selected == kiwicoinTab) {
            return Exchange.KIWICOIN;
        } else if (selected == cryptopiaTab) {
            return Exchange.CRYPTOPIA;
        } else {
            throw new AssertionError("Missing tab");
        }
    }

    public TradePlatform getSelectedPlatform() {
        Exchange selectedExchange = getSelectedExchange();
        return new TradePlatform(selectedExchange, getController(selectedExchange).getCurrencyPair());
    }

    public ExchangePaneController getController(Exchange exchange) {
        switch (exchange) {
            case WEX:
                return wexCtrl;
            case BITSTAMP:
                return bitstampCtrl;
            case COINBASE:
                return coinbaseCtrl;
            case KIWICOIN:
                return kiwicoinCtrl;
            case CRYPTOPIA:
                return cryptopiaCtrl;
        }
        throw new AssertionError("Missing controller");
    }

    public void logMessage(String message, String label) {
        if (label == null || label.isEmpty()) {
            settingsCtrl.logMessage(message);
        } else {
            settingsCtrl.logMessage(String.format("[%s] %s", label, message));
        }
    }

    public void logMessage(String message) {
        logMessage(message, null);
    }

    public static Dialog<Authorisation> loadAccountDialog() {
        Dialog<Authorisation> dialog = new Dialog<>();
        dialog.setTitle("Account login");
        dialog.setHeaderText("Enter key and secret");

        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 10, 10));

        TextField key = new TextField();
        PasswordField secret = new PasswordField();
        key.setPrefWidth(300);

        grid.add(new Label("Key:"), 0, 0);
        grid.add(key, 1, 0);
        grid.add(new Label("Secret:"), 0, 1);
        grid.add(secret, 1, 1);

        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);
        loginButton.disableProperty().bind(key.textProperty().isEmpty().or(secret.textProperty().isEmpty()));

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(key::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Authorisation(key.getText(), secret.getText());
            }
            return null;
        });
        return dialog;
    }
}
