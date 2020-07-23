package bx.cryptogui.control;

import bx.cryptogui.Utils;
import bx.cryptogui.data.*;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class TableColumnFactories {

    public static final Callback<TableColumn.CellDataFeatures<Trade, Long>, ObservableValue<Long>> TIME_VALUE_FACTORY =
            new Callback<TableColumn.CellDataFeatures<Trade, Long>, ObservableValue<Long>>() {
                @Override
                public ObservableValue<Long> call(TableColumn.CellDataFeatures<Trade, Long> param) {
                    return new SimpleObjectProperty<Long>(param.getValue().getTime());
                }
            };

    public static final Callback<TableColumn.CellDataFeatures<Trade, Number>, ObservableValue<Number>> PRICE_VALUE_FACTORY =
            new Callback<TableColumn.CellDataFeatures<Trade, Number>, ObservableValue<Number>>() {
                @Override
                public ObservableValue<Number> call(TableColumn.CellDataFeatures<Trade, Number> param) {
                    return new SimpleDoubleProperty(param.getValue().getPrice());
                }
            };

    public static final Callback<TableColumn.CellDataFeatures<Transaction, Transaction>, ObservableValue<Transaction>> TRANSACTION_PRICE_VALUE_FACTORY =
            new Callback<TableColumn.CellDataFeatures<Transaction, Transaction>, ObservableValue<Transaction>>() {
                @Override
                public ObservableValue<Transaction> call(TableColumn.CellDataFeatures<Transaction, Transaction> param) {
                    return new SimpleObjectProperty<>(param.getValue());
                }
            };

    public static final Callback<TableColumn.CellDataFeatures<Trade, Number>, ObservableValue<Number>> VOLUME_VALUE_FACTORY =
            new Callback<TableColumn.CellDataFeatures<Trade, Number>, ObservableValue<Number>>() {
                @Override
                public ObservableValue<Number> call(TableColumn.CellDataFeatures<Trade, Number> param) {
                    return new SimpleDoubleProperty(param.getValue().getVolume());
                }
            };

    public static final Callback<TableColumn.CellDataFeatures<Trade, Number>, ObservableValue<Number>> PRICED_VOLUME_VALUE_FACTORY =
            new Callback<TableColumn.CellDataFeatures<Trade, Number>, ObservableValue<Number>>() {
                @Override
                public ObservableValue<Number> call(TableColumn.CellDataFeatures<Trade, Number> param) {
                    return new SimpleDoubleProperty(param.getValue().getQuoteVolume());
                }
            };

    public static final Callback<TableColumn.CellDataFeatures<Trade, String>, ObservableValue<String>> CURRENCY_PAIR_VALUE_FACTORY =
            new Callback<TableColumn.CellDataFeatures<Trade, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<Trade, String> param) {
                    TradePlatform platform = param.getValue().getTradePlatform();
                    if (platform == null) {
                        return new SimpleStringProperty("???");
                    } else {
                        CurrencyPair pair = platform.getCurrencyPair();
                        return new SimpleStringProperty(pair.getBaseCurrency() + "/" + pair.getQuoteCurrency());
                    }
                }
            };

    public static final Callback<TableColumn<Trade, Long>, TableCell<Trade, Long>> TIME_FACTORY =
            new Callback<TableColumn<Trade, Long>, TableCell<Trade, Long>>() {
                @Override
                public TableCell<Trade, Long> call(TableColumn<Trade, Long> param) {
                    return new TableCell<Trade, Long>() {
                        @Override
                        protected void updateItem(Long item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                            } else {
                                setText(Utils.formatTime(item));
                            }
                        }
                    };
                }
            };

    public static final Callback<TableColumn<Trade, Number>, TableCell<Trade, Number>> PRICE_FACTORY =
            new Callback<TableColumn<Trade, Number>, TableCell<Trade, Number>>() {
                @Override
                public TableCell<Trade, Number> call(TableColumn<Trade, Number> param) {
                    return new TableCell<Trade, Number>() {
                        @Override
                        protected void updateItem(Number item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                            } else {
                                setText(Utils.formatDecimal(item.doubleValue(), 8, false));
                            }
                        }
                    };
                }
            };

    public static final Callback<TableColumn<Transaction, Transaction>, TableCell<Transaction, Transaction>> TRANSACTION_PRICE_FACTORY =
            new Callback<TableColumn<Transaction, Transaction>, TableCell<Transaction, Transaction>>() {
                @Override
                public TableCell<Transaction, Transaction> call(TableColumn<Transaction, Transaction> param) {
                    return new TableCell<Transaction, Transaction>() {
                        @Override
                        protected void updateItem(Transaction item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                            } else {
                                setText(Utils.formatDecimal(item.getPrice(), 8, false));
                                if (item.getTradeType() == 0) {
                                    setStyle("-fx-text-fill: -price-rise");
                                } else if (item.getTradeType() == 1) {
                                    setStyle("-fx-text-fill: -price-fall");
                                } else {
                                    setStyle("-fx-text-fill: yellow");
                                }
                            }
                        }
                    };
                }
            };

    public static final Callback<TableColumn<Trade, Number>, TableCell<Trade, Number>> VOLUME_FACTORY =
            new Callback<TableColumn<Trade, Number>, TableCell<Trade, Number>>() {
                @Override
                public TableCell<Trade, Number> call(TableColumn<Trade, Number> param) {
                    return new TableCell<Trade, Number>() {
                        @Override
                        protected void updateItem(Number item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                            } else {
                                setText(Utils.formatDecimal(item.doubleValue(), 8, true));
                            }
                        }
                    };
                }
            };

    public static final Callback<TableColumn<Trade, Number>, TableCell<Trade, Number>> PRICED_VOLUME_FACTORY =
            new Callback<TableColumn<Trade, Number>, TableCell<Trade, Number>>() {
                @Override
                public TableCell<Trade, Number> call(TableColumn<Trade, Number> param) {
                    return new TableCell<Trade, Number>() {
                        @Override
                        protected void updateItem(Number item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                            } else {
                                setText(Utils.formatDecimal(item.doubleValue(), 8, true));
                            }
                        }
                    };
                }
            };

    public static final Callback<TableColumn.CellDataFeatures<Order, String>, ObservableValue<String>> ORDER_ID_VALUE_FACTORY =
            new Callback<TableColumn.CellDataFeatures<Order, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<Order, String> param) {
                    return new SimpleStringProperty(String.valueOf(param.getValue().getId()));
                }
            };
}
