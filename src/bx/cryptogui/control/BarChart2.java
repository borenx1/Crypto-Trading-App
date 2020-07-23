package bx.cryptogui.control;

import javafx.animation.FadeTransition;
import javafx.beans.NamedArg;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.*;

public class BarChart2 extends XYChart<Number, Number> {

    private static final String NEGATIVE_STYLE = "negative";
    private static final int DEFAULT_INTERVAL = 60;

    private IntegerProperty interval = new IntegerPropertyBase(DEFAULT_INTERVAL) {
        @Override
        protected void invalidated() {
            requestChartLayout();
        }

        @Override
        public Object getBean() {
            return BarChart2.this;
        }

        @Override
        public String getName() {
            return "interval";
        }
    };
    public final int getInterval() {
        return interval.get();
    }
    public final void setInterval(int interval) {
        this.interval.set(interval);
    }
    public final IntegerProperty intervalProperty() {
        return interval;
    }

    private DoubleProperty barWidthProportion = new DoublePropertyBase(0.85) {
        @Override
        protected void invalidated() {
            requestChartLayout();
        }

        @Override
        public Object getBean() {
            return BarChart2.this;
        }

        @Override
        public String getName() {
            return "barWidthProportion";
        }
    };
    /**
     * Get the bar width proportion, where 1.0 is the distance between 2 bars.
     * @return bar width proportion
     */
    public final double getBarWidthProportion() {
        return barWidthProportion.get();
    }
    /**
     * Set the bar width proportion, where 1.0 is the distance between 2 bars.
     * @param width bar width proportion
     */
    public final void setBarWidthProportion(double width) {
        barWidthProportion.set(width);
    }
    /**
     * Get the bar width proportion property, where 1.0 is the distance between 2 bars.
     * @return bar width proportion property
     */
    public final DoubleProperty barWidthProportionProperty() {
        return barWidthProportion;
    }

    private ObjectProperty<BarAlignment> barPosition = new SimpleObjectProperty<>(this, "barPosition", BarAlignment.CENTRE);
    public final BarAlignment getBarPosition() {
        return barPosition.get();
    }
    public final void setBarPosition(BarAlignment barPosition) {
        this.barPosition.set(barPosition);
    }
    public final ObjectProperty<BarAlignment> barPositionProperty() {
        return barPosition;
    }

    /**
     * Construct a new CandleStickChart with the given axis.
     *
     * @param xAxis The x axis to use
     * @param yAxis The y axis to use
     */
    public BarChart2(@NamedArg("xAxis") Axis<Number> xAxis, @NamedArg("yAxis") Axis<Number> yAxis) {
        super(xAxis, yAxis);
        final String barChartCss = getClass().getResource("BarChart2.css").toExternalForm();
        getStylesheets().add(barChartCss);
        setData(FXCollections.observableArrayList());
        setAnimated(false);
//        xAxis.setAnimated(false);
//        yAxis.setAnimated(false);
        setVerticalZeroLineVisible(false);
    }

    /**
     * Construct a new CandleStickChart with the given axis and data.
     *
     * @param xAxis The x axis to use
     * @param yAxis The y axis to use
     * @param data The actual data list to use so changes will be
     *             reflected in the chart.
     */
    public BarChart2(@NamedArg("xAxis") Axis<Number> xAxis, @NamedArg("yAxis") Axis<Number> yAxis,
                     @NamedArg("data") ObservableList<Series<Number, Number>> data) {
        this(xAxis, yAxis);
        setData(data);
    }

    /** Called to update and layout the content for the plot */
    @Override protected void layoutPlotChildren() {
        // we have nothing to layout if no data is present
        if (getData() == null) {
            return;
        }
        Axis<Number> xAxis = getXAxis();
        Axis<Number> yAxis = getYAxis();
        // calculate candle width
        int intervalWidth = interval.get() > 0 ? interval.get() : DEFAULT_INTERVAL;
        double barWidth = Math.abs(barWidthProportion.get()*
                (xAxis.getDisplayPosition(intervalWidth) - xAxis.getDisplayPosition(0)));
        // update bar positions
        for (int index = 0; index < getData().size(); index++) {
            Series<Number, Number> series = getData().get(index);
            Iterator<Data<Number, Number>> iter = getDisplayedDataIterator(series);
            while (iter.hasNext()) {
                XYChart.Data<Number, Number> item = iter.next();
                Node itemNode = item.getNode();

                BarAlignment barPos = getBarPosition();
                double x = xAxis.getDisplayPosition(getCurrentDisplayedXValue(item));
                double y = yAxis.getDisplayPosition(getCurrentDisplayedYValue(item));
                double top = Math.max(y, yAxis.getDisplayPosition(0));
                double bottom = Math.min(y, yAxis.getDisplayPosition(0));
                // resize relocate bar
                itemNode.resize(barWidth, top-bottom);
                if (barPos == BarAlignment.LEFT) {
                    itemNode.relocate(x, y);
                } else if (barPos == BarAlignment.RIGHT) {
                    itemNode.relocate(x-barWidth, y);
                } else {
                    itemNode.relocate(x-barWidth/2, y);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override protected void dataItemChanged(Data<Number, Number> item) {
        if (item.getYValue().doubleValue() > 0) {
            item.getNode().getStyleClass().remove(NEGATIVE_STYLE);
        } else if (item.getYValue().doubleValue() < 0 && !item.getNode().getStyleClass().contains(NEGATIVE_STYLE)) {
            item.getNode().getStyleClass().add(NEGATIVE_STYLE);
        }
    }

    @Override protected void dataItemAdded(Series<Number, Number> series, int itemIndex, Data<Number, Number> item) {
        Node bar = createBar(getData().indexOf(series), item, itemIndex);
        if (shouldAnimate()) {
            bar.setOpacity(0);
            getPlotChildren().add(bar);
            // fade in new candle
            final FadeTransition ft = new FadeTransition(Duration.millis(500), bar);
            ft.setToValue(1);
            ft.play();
        } else {
            getPlotChildren().add(bar);
        }
    }

    @Override protected void dataItemRemoved(Data<Number, Number> item,
                                             Series<Number, Number> series) {
        final Node bar = item.getNode();
        if (shouldAnimate()) {
            // fade out old candle
            final FadeTransition ft = new FadeTransition(Duration.millis(500), bar);
            ft.setToValue(0);
            ft.setOnFinished((ActionEvent actionEvent) -> {
                getPlotChildren().remove(bar);
            });
            ft.play();
        } else {
            getPlotChildren().remove(bar);
        }
    }

    @Override protected void seriesAdded(Series<Number, Number> series,
                                         int seriesIndex) {
        // handle any data already in series
        for (int j = 0; j < series.getData().size(); j++) {
            XYChart.Data<Number, Number> item = series.getData().get(j);
            Node bar = createBar(seriesIndex, item, j);
            if (shouldAnimate()) {
                bar.setOpacity(0);
                getPlotChildren().add(bar);
                // fade in new bar
                final FadeTransition ft = new FadeTransition(Duration.millis(500), bar);
                ft.setToValue(1);
                ft.play();
            } else {
                getPlotChildren().add(bar);
            }
        }
    }

    @Override protected void seriesRemoved(Series<Number, Number> series) {
        // remove all candle nodes
        for (XYChart.Data<Number, Number> d : series.getData()) {
            final Node bar = d.getNode();
            if (shouldAnimate()) {
                // fade out old candle
                final FadeTransition ft = new FadeTransition(Duration.millis(500), bar);
                ft.setToValue(0);
                ft.setOnFinished((ActionEvent actionEvent) -> {
                    getPlotChildren().remove(bar);
                });
                ft.play();
            } else {
                getPlotChildren().remove(bar);
            }
        }
    }

    private Node createBar(int seriesIndex, final XYChart.Data<Number, Number> item, int itemIndex) {
        Node bar = item.getNode();
        if (bar == null) {
            bar = new StackPane();
            item.setNode(bar);
        }
        bar.getStyleClass().setAll("chart-bar", "series" + seriesIndex, "data" + itemIndex);
        if (item.getYValue().doubleValue() < 0) {
            bar.getStyleClass().add(NEGATIVE_STYLE);
        }
        return bar;
    }

    /**
     * This is called when the range has been invalidated and we need to
     * update it. If the axis are auto ranging then we compile a list of
     * all data that the given axis has to plot and call invalidateRange()
     * on the axis passing it that data.
     */
    @Override
    protected void updateAxisRange() {
        final Axis<Number> xa = getXAxis();
        final Axis<Number> ya = getYAxis();
        List<Double> xData = null;
        List<Number> yData = null;
        if (xa.isAutoRanging()) {
            xData = new ArrayList<>();
        }
        if (ya.isAutoRanging()) {
            yData = new ArrayList<>();
        }
        if (xData != null || yData != null) {
            for (XYChart.Series<Number, Number> series : getData()) {
                for (XYChart.Data<Number, Number> data : series.getData()) {
                    if (xData != null) {
                        xData.add(data.getXValue().doubleValue());
                    }
                    if (yData != null) {
                        yData.add(data.getYValue());
                    }
                }
            }
            if (xData != null) {
                if (xData.isEmpty()) {
                    xa.invalidateRange(new ArrayList<>());
                } else {
                    BarAlignment barPos = getBarPosition();
                    double min, max;
                    if (barPos == BarAlignment.LEFT) {
                        min = Collections.min(xData);
                        max = Collections.max(xData) + (interval.get() > 0 ? interval.get() : DEFAULT_INTERVAL);
                    } else if (barPos == BarAlignment.RIGHT) {
                        min = Collections.min(xData) - (interval.get() > 0 ? interval.get() : DEFAULT_INTERVAL);
                        max = Collections.max(xData);
                    } else {    // CENTER or null
                        min = Collections.min(xData) - (interval.get() > 0 ? interval.get() : DEFAULT_INTERVAL);
                        max = Collections.max(xData) + (interval.get() > 0 ? interval.get() : DEFAULT_INTERVAL);
                    }
                    xa.invalidateRange(Arrays.asList(min, max));
                }
            }
            if (yData != null) {
                ya.invalidateRange(yData);
            }
        }
    }

    public enum BarAlignment {
        LEFT, CENTRE, RIGHT
    }
}
