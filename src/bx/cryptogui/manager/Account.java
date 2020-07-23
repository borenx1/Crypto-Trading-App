package bx.cryptogui.manager;

import bx.cryptogui.data.Currency;
import bx.cryptogui.data.Exchange;
import bx.cryptogui.data.Order;
import bx.cryptogui.data.Transaction;
import bx.cryptogui.exchangeapi.Authorisation;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

import java.util.HashMap;
import java.util.HashSet;

public class Account {

    private BooleanProperty authInvalid = new SimpleBooleanProperty(false);
    public final boolean isAuthInvalid() {
        return authInvalid.get();
    }
    public final void setAuthInvalid(boolean value) {
        authInvalid.set(value);
    }
    public final BooleanProperty authInvalidProperty() {
        return authInvalid;
    }

    private LongProperty lastUpdated = new SimpleLongProperty(0);
    public final long getLastUpdated() {
        return lastUpdated.get();
    }
    public final void setLastUpdated(long value) {
        lastUpdated.set(value);
    }
    public final LongProperty lastUpdatedProperty() {
        return lastUpdated;
    }

    private ReadOnlySetWrapper<Currency> supportedCurrencies = new ReadOnlySetWrapper<>(FXCollections.observableSet(new HashSet<>()));
    public final ObservableSet<Currency> getSupportedCurrencies() {
        return supportedCurrencies.getReadOnlyProperty();
    }

    private ReadOnlyMapWrapper<Currency, Double> balance = new ReadOnlyMapWrapper<>(FXCollections.observableMap(new HashMap<>()));
    public final ObservableMap<Currency, Double> getBalance() {
        return balance.getReadOnlyProperty();
    }

    private final ReadOnlyListWrapper<Order> openOrders = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    public final ObservableList<Order> getOpenOrders() {
        return openOrders.get();
    }
    public final ReadOnlyListProperty<Order> openOrdersProperty() {
        return openOrders.getReadOnlyProperty();
    }

    private final ReadOnlyListWrapper<Transaction> pastTrades = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    public final ObservableList<Transaction> getPastTrades() {
        return pastTrades.get();
    }
    public final ReadOnlyListProperty<Transaction> pastTradesProperty() {
        return pastTrades.getReadOnlyProperty();
    }

    private final Authorisation authorisation;
    private final Exchange exchange;
    private final String name;

    public Account(Authorisation auth, Exchange exchange, String name) {
        this.authorisation = auth;
        this.exchange = exchange;
        this.name = name == null ? "" : name;
    }

    public Account(Authorisation auth, Exchange exchange) {
        this(auth, exchange, null);
    }

    public Account(String key, String secret, Exchange exchange) {
        this(new Authorisation(key, secret), exchange);
    }

    public void addSupportedCurrency(Currency...currencies) {
        for (Currency currency: currencies) {
            supportedCurrencies.get().add(currency);
            balance.get().putIfAbsent(currency, 0.0);
        }
    }

    public boolean supportsCurrency(Currency currency) {
        return supportedCurrencies.get().contains(currency);
    }

    public final Authorisation getAuthorisation() {
        return authorisation;
    }

    public final Exchange getExchange() {
        return exchange;
    }

    public final String getName() {
        return name;
    }

}
