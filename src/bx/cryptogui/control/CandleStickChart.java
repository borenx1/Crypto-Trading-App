/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package bx.cryptogui.control;

import java.util.*;

import javafx.animation.FadeTransition;
import javafx.beans.NamedArg;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.shape.Path;
import javafx.util.Duration;

/**
 * A candlestick chart is a style of bar-chart used primarily to describe
 * price movements of a security, derivative, or currency over time.
 *
 * The Data Y value is used for the opening price and then the
 * close, high and low values are stored in the Data's
 * extra value property using a CandleStickValues object.
 */
public class CandleStickChart extends XYChart<Number, Number> {

    private IntegerProperty interval = new IntegerPropertyBase(3600) {
        @Override
        protected void invalidated() {
            requestChartLayout();
        }

        @Override
        public Object getBean() {
            return CandleStickChart.this;
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

    private DoubleProperty candleWidthProportion = new DoublePropertyBase(0.85) {
        @Override
        protected void invalidated() {
            requestChartLayout();
        }

        @Override
        public Object getBean() {
            return CandleStickChart.this;
        }

        @Override
        public String getName() {
            return "candleWidthProportion";
        }
    };
    /**
     * Get the candle width proportion, where 1.0 is the distance between 2 candles.
     * @return candle width proportion
     */
    public final double getCandleWidthProportion() {
        return candleWidthProportion.get();
    }
    /**
     * Set the candle width proportion, where 1.0 is the distance between 2 candles.
     * @param width candle width proportion
     */
    public final void setCandleWidthProportion(double width) {
        candleWidthProportion.set(width);
    }
    /**
     * Get the candle width proportion property, where 1.0 is the distance between 2 candles.
     * @return candle width proportion property
     */
    public final DoubleProperty candleWidthProportionProperty() {
        return candleWidthProportion;
    }


    /**
     * Construct a new CandleStickChart with the given axis.
     *
     * @param xAxis The x axis to use
     * @param yAxis The y axis to use
     */
    public CandleStickChart(@NamedArg("xAxis") Axis<Number> xAxis, @NamedArg("yAxis") Axis<Number> yAxis) {
        super(xAxis, yAxis);
        final String candleStickChartCss =
            getClass().getResource("CandleStickChart.css").toExternalForm();
        getStylesheets().add(candleStickChartCss);
        setData(FXCollections.observableArrayList());
        setAnimated(false);
//        xAxis.setAnimated(false);
//        yAxis.setAnimated(false);
        setHorizontalZeroLineVisible(false);
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
    public CandleStickChart(@NamedArg("xAxis") Axis<Number> xAxis, @NamedArg("yAxis") Axis<Number> yAxis,
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
        int intervalWidth = interval.get() > 0 ? interval.get() : 3600;
        double candleWidth = Math.abs(candleWidthProportion.get()*
                (xAxis.getDisplayPosition(intervalWidth) - xAxis.getDisplayPosition(0)));
        // update candle positions
        for (int index = 0; index < getData().size(); index++) {
            Series<Number, Number> series = getData().get(index);
            Iterator<XYChart.Data<Number, Number>> iter = getDisplayedDataIterator(series);
            while (iter.hasNext()) {
                XYChart.Data<Number, Number> item = iter.next();
                Node itemNode = item.getNode();
                CandleStickValues extra = (CandleStickValues) item.getExtraValue();
                if (itemNode instanceof Candle && extra != null) {
                    double x = xAxis.getDisplayPosition(getCurrentDisplayedXValue(item));
                    double y = yAxis.getDisplayPosition(getCurrentDisplayedYValue(item));
                    double close = yAxis.getDisplayPosition(extra.getClose());
                    double high = yAxis.getDisplayPosition(extra.getHigh());
                    double low = yAxis.getDisplayPosition(extra.getLow());
                    // update candle
                    Candle candle = (Candle) itemNode;
                    candle.update(close - y, high - y, low - y, candleWidth);
                    candle.updateTooltip(item.getYValue().doubleValue(), extra.getClose(), extra.getHigh(), extra.getLow());

                    // position the candle
                    candle.setLayoutX(x);
                    candle.setLayoutY(y);
                }
            }
        }
    }

    @Override protected void dataItemChanged(Data<Number, Number> item) {
    }

    @Override protected void dataItemAdded(Series<Number, Number> series, int itemIndex, Data<Number, Number> item) {
        Node candle = createCandle(getData().indexOf(series), item, itemIndex);
        if (shouldAnimate()) {
            candle.setOpacity(0);
            getPlotChildren().add(candle);
            // fade in new candle
            final FadeTransition ft =
                new FadeTransition(Duration.millis(500), candle);
            ft.setToValue(1);
            ft.play();
        } else {
            getPlotChildren().add(candle);
        }
    }

    @Override protected void dataItemRemoved(Data<Number, Number> item, Series<Number, Number> series) {
        final Node candle = item.getNode();
        if (shouldAnimate()) {
            // fade out old candle
            final FadeTransition ft =
                new FadeTransition(Duration.millis(500), candle);
            ft.setToValue(0);
            ft.setOnFinished((ActionEvent actionEvent) -> {
                getPlotChildren().remove(candle);
            });
            ft.play();
        } else {
            getPlotChildren().remove(candle);
        }
    }

    @Override protected void seriesAdded(Series<Number, Number> series, int seriesIndex) {
        // handle any data already in series
        for (int j = 0; j < series.getData().size(); j++) {
            XYChart.Data item = series.getData().get(j);
            Node candle = createCandle(seriesIndex, item, j);
            if (shouldAnimate()) {
                candle.setOpacity(0);
                getPlotChildren().add(candle);
                // fade in new candle
                final FadeTransition ft =
                    new FadeTransition(Duration.millis(500), candle);
                ft.setToValue(1);
                ft.play();
            } else {
                getPlotChildren().add(candle);
            }
        }
    }

    @Override protected void seriesRemoved(Series<Number, Number> series) {
        // remove all candle nodes
        for (XYChart.Data<Number, Number> d : series.getData()) {
            final Node candle = d.getNode();
            if (shouldAnimate()) {
                // fade out old candle
                final FadeTransition ft =
                    new FadeTransition(Duration.millis(500), candle);
                ft.setToValue(0);
                ft.setOnFinished((ActionEvent actionEvent) -> {
                    getPlotChildren().remove(candle);
                });
                ft.play();
            } else {
                getPlotChildren().remove(candle);
            }
        }
    }

    /**
     * Create a new Candle node to represent a single data item
     * @param seriesIndex The index of the series the data item is in
     * @param item        The data item to create node for
     * @param itemIndex   The index of the data item in the series
     * @return New candle node to represent the give data item
     */
    private Node createCandle(int seriesIndex, final XYChart.Data item, int itemIndex) {
        Node candle = item.getNode();
        // check if candle has already been created
        if (candle instanceof Candle) {
            ((Candle)candle).setSeriesAndDataStyleClasses("series" + seriesIndex,
                                                          "data" + itemIndex);
        } else {
            candle = new Candle("series" + seriesIndex, "data" + itemIndex);
            item.setNode(candle);
        }
        return candle;
    }

    /**
     * This is called when the range has been invalidated and we need to
     * update it. If the axis are auto ranging then we compile a list of
     * all data that the given axis has to plot and call invalidateRange()
     * on the axis passing it that data.
     */
    @Override
    protected void updateAxisRange() {
        // For candle stick chart we need to override this method as we need
        // to let the axis know that they need to be able to cover the area
        // occupied by the high to low range not just its center data value.
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
            for (XYChart.Series<Number, Number> series: getData()) {
                for (XYChart.Data<Number, Number> data: series.getData()) {
                    if (xData != null) {
                        xData.add(data.getXValue().doubleValue());
                    }
                    if (yData != null) {
                        CandleStickValues extras = (CandleStickValues) data.getExtraValue();
                        if (extras != null) {
                            yData.add(extras.getHigh());
                            yData.add(extras.getLow());
                        } else {
                            yData.add(data.getYValue());
                        }
                    }
                }
            }
            if (xData != null) {
                if (xData.isEmpty()) {
                    xa.invalidateRange(new ArrayList<>());
                } else {
                    double min = Collections.min(xData) - (interval.get() > 0 ? interval.get() : 3600);
                    double max = Collections.max(xData) + (interval.get() > 0 ? interval.get() : 3600);
                    xa.invalidateRange(Arrays.asList(min, max));
                }
            }
            if (yData != null) {
                ya.invalidateRange(yData);
            }
        }
    }
}