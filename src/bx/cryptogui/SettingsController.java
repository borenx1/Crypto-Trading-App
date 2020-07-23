package bx.cryptogui;

import bx.cryptogui.control.ExchangeUpdatePane;
import bx.cryptogui.manager.DatabaseManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.util.StringConverter;

import java.net.URL;
import java.time.Duration;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    private ReadOnlyIntegerWrapper maxBarCount = new ReadOnlyIntegerWrapper(500);
    public final int getMaxBarCount() {
        return maxBarCount.get();
    }
    public final ReadOnlyIntegerProperty maxBarCountProperty() {
        return maxBarCount.getReadOnlyProperty();
    }

    private LongProperty lastUpdated = new SimpleLongProperty(Long.MIN_VALUE);
    public final long getLastUpdated() {
        return lastUpdated.get();
    }
    public final void setLastUpdated(long value) {
        lastUpdated.set(value);
    }
    public final LongProperty lastUpdatedProperty() {
        return lastUpdated;
    }

    private ReadOnlyIntegerWrapper autoUpdateDatabaseRate = new ReadOnlyIntegerWrapper(180);
    public final int getAutoUpdateDatabaseRate() {
        return autoUpdateDatabaseRate.get();
    }
    public final ReadOnlyIntegerProperty autoUpdateDatabaseRateProperty() {
        return autoUpdateDatabaseRate.getReadOnlyProperty();
    }

    @FXML private Spinner<Integer> maxCandlesSpinner;
    @FXML private Button maxCandlesButton;

    @FXML private Label databaseUpdateText;
    @FXML private ProgressIndicator databaseUpdateProgress;
    private Timeline lastUpdatedTimeline;

    @FXML private Label updateRateText;
    @FXML private Slider updateSlider;
    @FXML private Label updateSliderText;
    @FXML private Button updateRateButton;

    @FXML protected ExchangeUpdatePane wexUpdatePane;
    @FXML protected ExchangeUpdatePane bitstampUpdatePane;
    @FXML protected ExchangeUpdatePane coinbaseUpdatePane;
    @FXML protected ExchangeUpdatePane cryptopiaUpdatePane;
    @FXML protected ExchangeUpdatePane kiwicoinUpdatePane;
    @FXML protected Button updateDatabaseButton;

    @FXML private TextArea messageLog;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // bind properties
        maxCandlesButton.disableProperty().bind(Bindings.createBooleanBinding(() -> getMaxBarCount() == maxCandlesSpinner.getValue(),
                maxBarCount, maxCandlesSpinner.valueProperty()));
        maxCandlesButton.setOnAction(event -> maxBarCount.set(maxCandlesSpinner.getValue()));

        // Last updated timer
        databaseUpdateText.textProperty().bind(Bindings.createStringBinding(() -> {
            if (getLastUpdated() == Long.MIN_VALUE) {
                return "Never";
            }
            return String.format("%s seconds ago", getLastUpdated());
        }, lastUpdatedProperty()));
        lastUpdatedTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> lastUpdated.set(getLastUpdated() + 1)));
        lastUpdatedTimeline.setCycleCount(Timeline.INDEFINITE);
        // Auto update rate property and controls
        updateRateText.textProperty().bind(autoUpdateDatabaseRateProperty().asString().concat(" seconds"));
        updateSliderText.textProperty().bind(Bindings.createStringBinding(() -> {
            return String.valueOf(Math.round(updateSlider.getValue())) + " seconds";
        }, updateSlider.valueProperty()));
        updateRateButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            return getAutoUpdateDatabaseRate() == Math.round(updateSlider.getValue());
        }, autoUpdateDatabaseRateProperty(), updateSlider.valueProperty()));
        updateRateButton.setOnAction(event -> autoUpdateDatabaseRate.set((int) Math.round(updateSlider.getValue())));
        updateSlider.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                updateRateButton.fire();
            }
        });
        // Master progress indicator
        databaseUpdateProgress.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            for (ExchangeUpdatePane pane: new ExchangeUpdatePane[] {wexUpdatePane, bitstampUpdatePane,
                    coinbaseUpdatePane, cryptopiaUpdatePane, kiwicoinUpdatePane}) {
                if (pane.isProgressVisible()) {
                    return true;
                }
            }
            return false;
        }, wexUpdatePane.progressVisibleProperty(), bitstampUpdatePane.progressVisibleProperty(),
                coinbaseUpdatePane.progressVisibleProperty(), cryptopiaUpdatePane.progressVisibleProperty(),
                kiwicoinUpdatePane.progressVisibleProperty()));
    }

    public void logMessage(String message) {
        final String newLine = Utils.formatTime(System.currentTimeMillis()/1000) + "  " + message;
        final String oldText = messageLog.getText();
        String newText = oldText + (oldText.endsWith("\n") || oldText.isEmpty() ? "" : "\n") + newLine;
        messageLog.setText(newText);
        // Scroll to bottom
        messageLog.selectEnd();
        messageLog.deselect();
    }

    public void resetLastUpdated() {
        lastUpdatedTimeline.stop();
        lastUpdated.set(0);
        lastUpdatedTimeline.playFromStart();
    }
}
