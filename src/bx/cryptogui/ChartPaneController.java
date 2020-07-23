package bx.cryptogui;

import bx.cryptogui.control.*;
import bx.cryptogui.data.Order;
import bx.cryptogui.data.Transaction;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.StringConverter;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ChartPaneController implements Initializable {

    public static final DateTimeFormatter INFO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE dd/MM/yyyy HH:mm:ss");
    public static final int MIN_DISPLAYED_BARS = 20;
    public static final int MAX_DISPLAYED_BARS = 300;

    private final DoubleProperty dragStartX = new SimpleDoubleProperty();
    private final IntegerProperty dragStartOffset = new SimpleIntegerProperty();

    private ReadOnlyIntegerWrapper displayedBars = new ReadOnlyIntegerWrapper(50) {
        @Override
        protected void invalidated() {
            if (displayedBars.get() < MIN_DISPLAYED_BARS) {
                displayedBars.set(MIN_DISPLAYED_BARS);
                System.err.println("WARNING: min displayed reached");
            } else if (displayedBars.get() > MAX_DISPLAYED_BARS) {
                displayedBars.set(MAX_DISPLAYED_BARS);
                System.err.println("WARNING: max displayed reached");
            }
        }
    };
    public final int getDisplayedBars() {
        return displayedBars.get();
    }
    public final ReadOnlyIntegerProperty displayedBarsProperty() {
        return displayedBars.getReadOnlyProperty();
    }

    private ReadOnlyIntegerWrapper firstBarOffset = new ReadOnlyIntegerWrapper(0) {
        @Override
        protected void invalidated() {
            if (getFirstBarOffset() < 0) {
                throw new AssertionError("Offset less than 0");
            }
        }
    };
    public final int getFirstBarOffset() {
        return firstBarOffset.get();
    }
    public final ReadOnlyIntegerProperty firstBarOffsetProperty() {
        return firstBarOffset.getReadOnlyProperty();
    }

    @FXML protected ChoiceBox<Duration> timeIntervalChoice;

    @FXML private CandleStickChart priceChart;
    @FXML private DateAxis tAxis;
    @FXML private NumberAxis yAxis;
    @FXML private BarChart2 volumeChart;
    @FXML private NumberAxis vAxis;
    private final XYChart.Series<Number, Number> priceData = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> volumeData = new XYChart.Series<>();

    @FXML private Crosshair crosshair;

    @FXML protected VBox chartLoadingPane;
    @FXML protected ProgressIndicator chartLoadingProgress;
    @FXML protected Label chartLoadingText;

    @FXML protected Text tradePlatformText;
    @FXML protected Text tickerPrice;
    @FXML protected Text tickerVolume;
    @FXML protected Text tickerTime;
    @FXML protected TableView<Order> buyOrders;
    @FXML protected TableView<Order> sellOrders;
    @FXML private TableView<Transaction> pastTrades;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        timeIntervalChoice.setConverter(new StringConverter<Duration>() {
            @Override
            public String toString(Duration object) {
                if (object.getSeconds() >= 86400*7) {
                    return object.toDays()/7 + "W";
                } else if (object.getSeconds() >= 86400) {
                    return object.toDays() + "D";
                } else if (object.getSeconds() >= 3600) {
                    return object.toHours() + "H";
                } else {
                    return object.toMinutes() + "m";
                }
            }
            @Override
            public Duration fromString(String string) {
                long num = Long.valueOf(string.substring(0, string.length() - 1));
                if (string.endsWith("W")) {
                    return Duration.ofDays(num*7);
                } else if (string.endsWith("D")) {
                    return Duration.ofDays(num);
                } else if (string.endsWith("H")) {
                    return Duration.ofHours(num);
                } else {
                    return Duration.ofMinutes(num);
                }
            }
        });
        timeIntervalChoice.getItems().setAll(Duration.ofMinutes(1), Duration.ofMinutes(2), Duration.ofMinutes(3), Duration.ofMinutes(5),
                Duration.ofMinutes(15), Duration.ofMinutes(30), Duration.ofHours(1), Duration.ofHours(2), Duration.ofHours(3),
                Duration.ofHours(6), Duration.ofHours(12), Duration.ofDays(1), Duration.ofDays(3), Duration.ofDays(7));
        timeIntervalChoice.getSelectionModel().select(Duration.ofHours(1));

        priceChart.getData().setAll(priceData);
        volumeChart.getData().setAll(volumeData);

        tAxis.setZoneOffset(Utils.getLocalZoneOffset());
        tAxis.setMonthFormatter(DateTimeFormatter.ofPattern("MMM"));
        tAxis.setDayFormatter(DateTimeFormatter.ofPattern("dd MMM"));
        tAxis.setHourFormatter(DateTimeFormatter.ofPattern("HH"));
        tAxis.setTimeFormatter(DateTimeFormatter.ofPattern("HH:mm"));
        // bind axes
        for (XYChart chart: new XYChart[] {priceChart}) {
            DateAxis xa = (DateAxis) chart.getXAxis();
            xa.autoRangingProperty().bind(tAxis.autoRangingProperty());
            xa.maxTickMarksProperty().bind(tAxis.maxTickMarksProperty());
            xa.lowerBoundProperty().bind(tAxis.lowerBoundProperty());
            xa.upperBoundProperty().bind(tAxis.upperBoundProperty());
            xa.zoneOffsetProperty().bind(tAxis.zoneOffsetProperty());
            // hide x axis
            xa.setTickMarkVisible(false);
            xa.setMinorTickVisible(false);
            xa.setTickLabelsVisible(false);
        }
        // align y axis with price chart
        for (XYChart chart: new XYChart[] {volumeChart}) {
            NumberAxis ya = (NumberAxis) chart.getYAxis();
            chart.prefWidthProperty().bind(priceChart.widthProperty());
            ya.prefWidthProperty().bind(yAxis.widthProperty());
        }
        // set chart heights
        volumeChart.prefHeightProperty().bind(priceChart.heightProperty().multiply(0.3));
        // setup crosshair
        crosshair.setVLabelOffset(10);
        crosshair.setMouseMovedHandler(event -> {
            double x = event.getX(), y = event.getY();
            double sceneX = event.getSceneX(), sceneY = event.getSceneY();
            // x value
            double timeLocalX = tAxis.sceneToLocal(sceneX, 0).getX();
            long snappedTime = getChartTimeHovered(timeLocalX);
            LocalDateTime dateTime = LocalDateTime.ofEpochSecond(snappedTime, 0, Utils.getLocalZoneOffset());
            String vText;
            switch (tAxis.getTickSeparation()) {
                case YEAR:
                case SIX_MONTHS:
                    vText = tAxis.getMonthFormatter().format(dateTime);
                    break;
                case THREE_MONTHS:
                case MONTH:
                case WEEK:
                    vText = tAxis.getDayFormatter().format(dateTime);
                    break;
                case DAY:
                case SIX_HOURS:
                    vText = tAxis.getHourFormatter().format(dateTime);
                    break;
                default:
                    vText = tAxis.getTimeFormatter().format(dateTime);
            }
            crosshair.setCrosshairX(timeAxis2Crosshair(tAxis.getDisplayPosition(snappedTime), 0).getX());
            crosshair.setVLabelText(vText);
            // y value
            double priceLocalY = yAxis.sceneToLocal(0, sceneY).getY();
            double volumeLocalY = vAxis.sceneToLocal(0, sceneY).getY();
            String hText;
            if (Utils.betweenInclusive(priceLocalY, yAxis.getBoundsInLocal().getMinY(), yAxis.getBoundsInLocal().getMaxY())) {
                Number price = yAxis.getValueForDisplay(priceLocalY);
                hText = String.format("%.1f", price.doubleValue());
            } else if (Utils.betweenInclusive(volumeLocalY, vAxis.getBoundsInLocal().getMinY(), vAxis.getBoundsInLocal().getMaxY())) {
                Number volume = vAxis.getValueForDisplay(volumeLocalY);
                hText = volume.doubleValue() >= 0.0 ? String.format("%.1f", volume.doubleValue()) : null;
            } else {
                hText = null;
            }
            crosshair.setCrosshairY(y);
            crosshair.setHLabelText(hText);

            XYChart.Data<Number, Number>[] hoveredData = getChartData(snappedTime);
            String infoText;
            if (hoveredData == null) {
                infoText = null;
            } else {
                double open = hoveredData[0].getYValue().doubleValue();
                CandleStickValues extraValues = (CandleStickValues) hoveredData[0].getExtraValue();
                double high = extraValues.getHigh();
                double low = extraValues.getLow();
                double close = extraValues.getClose();
                double volume = hoveredData[1].getYValue().doubleValue();
                double volume2 = (double) hoveredData[1].getExtraValue();
                infoText = String.format("%s%nO: %.3f  H: %.3f  L: %.3f  C: %.3f  CHANGE: %+.2f%%  AMPLITUDE: %.2f%%",
                        INFO_DATE_TIME_FORMATTER.format(LocalDateTime.ofEpochSecond(snappedTime, 0, Utils.getLocalZoneOffset())),
                        open, high, low, close, (close-open)/open*100, (high-low)/low*100);
                infoText += String.format("%nVOLUME: %s / %s", Utils.formatDecimal(volume, 8, true),
                        Utils.formatDecimal(volume2, 8, true));
            }
            crosshair.setInfoText(infoText);
        });
        crosshair.setOnScroll(event -> {
            double delta = event.getDeltaY();
            if (delta < 0) {
                displayedBars.set(Math.min(getDisplayedBars() + (int) Math.ceil(getDisplayedBars()*0.1), MAX_DISPLAYED_BARS));
                refreshTimeAxis();
            } else if (delta > 0) {
                displayedBars.set(Math.max(getDisplayedBars() - (int) Math.ceil(getDisplayedBars()*0.1), MIN_DISPLAYED_BARS));
                refreshTimeAxis();
            }
        });
        crosshair.setOnMousePressed(event -> {
            dragStartX.set(event.getX());
            dragStartOffset.set(getFirstBarOffset());
        });
        crosshair.setOnMouseDragged(event -> {
            if (event.isPrimaryButtonDown()) {
                int interval = priceChart.getInterval();
                int offsetShift = (int) (tAxis.getValueForDisplay(event.getX()) - tAxis.getValueForDisplay(dragStartX.get())) / interval;
                firstBarOffset.set(Math.max(0, dragStartOffset.get() + offsetShift));
                refreshTimeAxis();
            }
        });
    }

    public void updateData(final List<Transaction> data, final List<XYChart.Data<Number, Number>> prices,
                              final List<XYChart.Data<Number, Number>> volumes, int interval) {
        assert Platform.isFxApplicationThread();
        priceData.getData().setAll(prices);
        volumeData.getData().setAll(volumes);
        // manually update parameters
        priceChart.setInterval(interval);
        volumeChart.setInterval(interval);
        firstBarOffset.set(0);
        refreshTimeAxis();
        for (int i = 0, max = prices.size(); i < max; i++) {
            double open = prices.get(i).getYValue().doubleValue();
            double close = ((CandleStickValues) prices.get(i).getExtraValue()).getClose();
            ObservableList<String> styleClass = volumes.get(i).getNode().getStyleClass();
            styleClass.removeAll("open-above-close", "close-above-close");
            if (open > close) {
                styleClass.add("open-above-close");
            } else {
                styleClass.add("close-above-open");
            }
        }
        // past trades
        pastTrades.getItems().clear();
        for (int i = data.size() - 1, count = 0; i >= 0 && count < 200; i--, count++) {
            pastTrades.getItems().add(data.get(i));
        }
        pastTrades.scrollTo(0);
    }

    public void refreshTimeAxis() {
        assert !tAxis.isAutoRanging();
//        assert !yAxis.isAutoRanging();
        int totalBars = priceData.getData().size();
        if (totalBars == 0) {   // Do nothing if no data
            return;
        }
        if (getFirstBarOffset() + getDisplayedBars() > totalBars) {
            // showing empty bars on the left
            if (getDisplayedBars() > totalBars) {
                firstBarOffset.set(0);
                displayedBars.set(Math.max(totalBars, MIN_DISPLAYED_BARS));
            } else {
                firstBarOffset.set(totalBars - getDisplayedBars());
            }
        }
        long interval = priceChart.getInterval();
//        System.out.println(String.format("total=%s, offset=%s, displayed=%s", totalBars, getFirstBarOffset(), getDisplayedBars()));
        int firstTimeIndex = Math.max(0, totalBars - getFirstBarOffset() - getDisplayedBars());
        int lastTimeIndex = totalBars - getFirstBarOffset() - 1;

        long firstTIme = priceData.getData().get(firstTimeIndex).getXValue().longValue();
        long lastTime = priceData.getData().get(lastTimeIndex).getXValue().longValue();
        tAxis.setLowerBound(firstTIme-interval/2);
        tAxis.setUpperBound(lastTime+interval/2);

        double minPrice = Double.MAX_VALUE, maxPrice = -Double.MAX_VALUE;
        double maxVolume = -Double.MAX_VALUE;
        for (int i = firstTimeIndex; i <= lastTimeIndex; i++) {
            minPrice = Math.min(minPrice, ((CandleStickValues) priceData.getData().get(i).getExtraValue()).getLow());
            maxPrice = Math.max(maxPrice, ((CandleStickValues) priceData.getData().get(i).getExtraValue()).getHigh());
            maxVolume = Math.max(maxVolume, volumeData.getData().get(i).getYValue().doubleValue());
        }
        double priceRange = maxPrice - minPrice;
        yAxis.setLowerBound(Math.max(0.0, Math.floor(minPrice - 0.05*priceRange)));
        yAxis.setUpperBound(Math.ceil(maxPrice + 0.05*priceRange));
        yAxis.setTickUnit(Math.floor(priceRange/10));
        vAxis.setLowerBound(0);
        vAxis.setUpperBound(Math.ceil(maxVolume*1.05));
        vAxis.setTickUnit(Math.floor(maxVolume/4));
    }

    public void setPriceAxisRange(double lower, double upper, double tickUnit) {
        if (yAxis.isAutoRanging()) {
            yAxis.setAutoRanging(false);
        }
        yAxis.setLowerBound(lower);
        yAxis.setUpperBound(upper);
        yAxis.setTickUnit(tickUnit);
    }

    /**
     * Get the the closest time (x) value from the chart that the mouse is hovering on.
     * @return hovered time value
     */
    private long getChartTimeHovered(double localX) {
        // actual hovered time
        long time = tAxis.getValueForDisplay(localX);
        // snapped to closest data X
        List<Long> plottedTimes = priceData.getData().stream().map(
                data -> data.getXValue().longValue()).collect(Collectors.toList());
        return Utils.getClosestValue(time, plottedTimes);
    }

    private XYChart.Data<Number, Number>[] getChartData(long time) {
        for (int i = 0; i < priceData.getData().size(); i++) {
            if (time == priceData.getData().get(i).getXValue().longValue()) {
                return new XYChart.Data[] {priceData.getData().get(i), volumeData.getData().get(i)};
            }
        }
        return null;
    }

    /**
     * Converts time axis local coordinates to crosshair local coordinates.
     * @param x time axis x
     * @param y time axis y
     * @return crosshair local coordinates
     */
    private Point2D timeAxis2Crosshair(double x, double y) {
        Point2D sceneCoordinates = tAxis.localToScene(x, y);
        return crosshair.sceneToLocal(sceneCoordinates);
    }

    /**
     * Converts crosshair local coordinates to time axis local coordinates.
     * @param x crosshair x
     * @param y crosshair y
     * @return time axis local coordinates
     */
    private Point2D crosshair2TimeAxis(double x, double y) {
        Point2D sceneCoordinates = crosshair.localToScene(x, y);
        return tAxis.sceneToLocal(sceneCoordinates);
    }
}
