package bx.cryptogui.control;

import bx.cryptogui.Utils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class ExchangeUpdatePane extends HBox {

    private LongProperty time = new SimpleLongProperty(Long.MIN_VALUE);
    public final long getTime() {
        return time.get();
    }
    public final void setTime(long value) {
        time.set(value);
    }
    public final LongProperty timeProperty() {
        return time;
    }

    private IntegerProperty newRows = new SimpleIntegerProperty(0);
    public final int getNewRows() {
        return newRows.get();
    }
    public final void setNewRows(int value) {
        newRows.set(value);
    }
    public final IntegerProperty newRowsProperty() {
        return newRows;
    }

    private BooleanProperty progressVisible = new SimpleBooleanProperty(false);
    public final boolean isProgressVisible() {
        return progressVisible.get();
    }
    public final void setProgressVisible(boolean value) {
        progressVisible.set(value);
    }
    public final BooleanProperty progressVisibleProperty() {
        return progressVisible;
    }

    private final Label timeText = new Label();
    private final ProgressIndicator progress = new ProgressIndicator(-1);

    public ExchangeUpdatePane() {
        getChildren().addAll(timeText, progress);
        setAlignment(Pos.CENTER_LEFT);
        timeText.setMaxWidth(Double.POSITIVE_INFINITY);
        HBox.setHgrow(timeText, Priority.ALWAYS);
        progress.setPrefWidth(20);
        progress.setPrefHeight(20);
        timeText.textProperty().bind(Bindings.createStringBinding(() -> {
            long curTime = getTime();
            if (curTime == Long.MIN_VALUE) {
                return "Never";
            }
            return String.format("%s, %s new", Utils.formatDateTime(getTime()), getNewRows());
        }, timeProperty(), newRowsProperty()));
        progress.visibleProperty().bind(progressVisibleProperty());
    }
}
