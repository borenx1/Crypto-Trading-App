package bx.cryptogui.control;

import bx.cryptogui.Utils;
import com.sun.javafx.charts.ChartLayoutAnimator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.geometry.Dimension2D;
import javafx.scene.chart.Axis;
import javafx.scene.chart.ValueAxis;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DateAxis extends ValueAxis<Long> {

    public static final DateTimeFormatter DEFAULT_YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    public static final DateTimeFormatter DEFAULT_MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM");
    public static final DateTimeFormatter DEFAULT_DAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");
    public static final DateTimeFormatter DEFAULT_HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DEFAULT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // -------------- PRIVATE PROPERTIES -------------------------------------------------------------------------------


    // -------------- PUBLIC PROPERTIES --------------------------------------------------------------------------------

    private ReadOnlyObjectWrapper<TickSeparation> tickSeparation = new ReadOnlyObjectWrapper<>(this, "tickSeparation");
    public final TickSeparation getTickSeparation() {
        return tickSeparation.get();
    }
    public final ReadOnlyObjectProperty<TickSeparation> tickSeparationProperty() {
        return tickSeparation.getReadOnlyProperty();
    }

    private IntegerProperty maxTickMarks = new SimpleIntegerProperty(this, "maxTickMarks", 8) {
        @Override
        protected void invalidated() {
            if (!isAutoRanging()) {
                invalidateRange();
                requestAxisLayout();
            }
        }
    };
    public final int getMaxTickMarks() {
        return maxTickMarks.get();
    }
    public final void setMaxTickMarks(int value) {
        maxTickMarks.set(value);
    }
    public final IntegerProperty maxTickMarksProperty() {
        return maxTickMarks;
    }

    private ObjectProperty<ZoneOffset> zoneOffset =
            new SimpleObjectProperty<ZoneOffset>(this, "zoneOffset") {
                @Override
                protected void invalidated() {
                    invalidateRange();
                    requestAxisLayout();
                }
    };
    public final ZoneOffset getZoneOffset() {
        return zoneOffset.get();
    }
    public final void setZoneOffset(ZoneOffset value) {
        zoneOffset.set(value);
    }
    public final ObjectProperty<ZoneOffset> zoneOffsetProperty() {
        return zoneOffset;
    }

    public ObjectProperty<DayOfWeek> firstDayOfWeek = new SimpleObjectProperty<DayOfWeek>(this, "firstDayOfWeek", DayOfWeek.MONDAY) {
        @Override
        protected void invalidated() {
            requestAxisLayout();
        }
    };
    public final DayOfWeek getFirstDayOfWeek() {
        return firstDayOfWeek.get();
    }
    public final void setFirstDayOfWeek(DayOfWeek value) {
        firstDayOfWeek.set(value);
    }
    public final ObjectProperty<DayOfWeek> firstDayOfWeekProperty() {
        return firstDayOfWeek;
    }

    private ObjectProperty<DateTimeFormatter> yearFormatter =
            new SimpleObjectProperty<DateTimeFormatter>(this, "yearFormatter", DEFAULT_YEAR_FORMATTER) {
                @Override
                protected void invalidated() {
                    requestAxisLayout();
                }
    };
    public final DateTimeFormatter getYearFormatter() {
        return yearFormatter.get();
    }
    public final void setYearFormatter(DateTimeFormatter value) {
        yearFormatter.set(value);
    }
    public final ObjectProperty<DateTimeFormatter> yearFormatterProperty() {
        return yearFormatter;
    }

    private ObjectProperty<DateTimeFormatter> monthFormatter =
            new SimpleObjectProperty<DateTimeFormatter>(this, "monthFormatter", DEFAULT_MONTH_FORMATTER) {
                @Override
                protected void invalidated() {
                    requestAxisLayout();
                }
            };
    public final DateTimeFormatter getMonthFormatter() {
        return monthFormatter.get();
    }
    public final void setMonthFormatter(DateTimeFormatter value) {
        monthFormatter.set(value);
    }
    public final ObjectProperty<DateTimeFormatter> monthFormatterProperty() {
        return monthFormatter;
    }

    private ObjectProperty<DateTimeFormatter> dayFormatter =
            new SimpleObjectProperty<DateTimeFormatter>(this, "dayFormatter", DEFAULT_DAY_FORMATTER) {
                @Override
                protected void invalidated() {
                    requestAxisLayout();
                }
            };
    public final DateTimeFormatter getDayFormatter() {
        return dayFormatter.get();
    }
    public final void setDayFormatter(DateTimeFormatter value) {
        dayFormatter.set(value);
    }
    public final ObjectProperty<DateTimeFormatter> dayFormatterProperty() {
        return dayFormatter;
    }

    private ObjectProperty<DateTimeFormatter> hourFormatter =
            new SimpleObjectProperty<DateTimeFormatter>(this, "hourFormatter", DEFAULT_HOUR_FORMATTER) {
                @Override
                protected void invalidated() {
                    requestAxisLayout();
                }
            };
    public final DateTimeFormatter getHourFormatter() {
        return hourFormatter.get();
    }
    public final void setHourFormatter(DateTimeFormatter value) {
        hourFormatter.set(value);
    }
    public final ObjectProperty<DateTimeFormatter> hourFormatterProperty() {
        return hourFormatter;
    }

    private ObjectProperty<DateTimeFormatter> timeFormatter =
            new SimpleObjectProperty<DateTimeFormatter>(this, "timeFormatter", DEFAULT_TIME_FORMATTER) {
                @Override
                protected void invalidated() {
                    requestAxisLayout();
                }
            };
    public final DateTimeFormatter getTimeFormatter() {
        return timeFormatter.get();
    }
    public final void setTimeFormatter(DateTimeFormatter value) {
        timeFormatter.set(value);
    }
    public final ObjectProperty<DateTimeFormatter> timeFormatterProperty() {
        return timeFormatter;
    }


    // -------------- CONSTRUCTORS -------------------------------------------------------------------------------------

    /**
     * Create an auto-ranging DateAxis
     */
    public DateAxis() {
        super();
        setMinorTickVisible(false);
    }

    /**
     * Create a non-auto-ranging DateAxis with the given upper & lower bound
     * @param lowerBound The lower bound for this axis, ie min plottable value
     * @param upperBound The upper bound for this axis, ie max plottable value
     */
    public DateAxis(double lowerBound, double upperBound) {
        super(lowerBound, upperBound);
        setMinorTickVisible(false);
    }

    // -------------- PRIVATE METHODS ----------------------------------------------------------------------------------

    private LocalDateTime getLocalDateTime(long epochSeconds) {
        ZoneOffset offset = getZoneOffset();
        if (offset == null) {
            offset = ZoneId.systemDefault().getRules().getOffset(Instant.ofEpochSecond(epochSeconds));
        }
        return LocalDateTime.ofEpochSecond(epochSeconds, 0, offset);
    }

    private long getEpochSeconds(LocalDateTime dateTime) {
        ZoneOffset offset = getZoneOffset();
        if (offset == null) {
            offset = ZoneId.systemDefault().getRules().getOffset(dateTime);
        }
        return dateTime.toEpochSecond(offset);
    }

    private List<Long> calculateTickValues(long lowerBound, long upperBound, TickSeparation tickSeparation) {
        assert tickSeparation != null;
        List<Long> tickValues = new ArrayList<>();
        if (TickSeparation.isConsistent(tickSeparation)) {
            long lastValue;
            long unit;
            if (tickSeparation == TickSeparation.SECOND) {
                lastValue = upperBound;
                unit = 1;
            } else if (tickSeparation == TickSeparation.FIVE_SECONDS) {
                lastValue = upperBound - getLocalDateTime(upperBound).getSecond() % 5;
                unit = 5;
            } else if (tickSeparation == TickSeparation.FIFTEEN_SECONDS) {
                lastValue = upperBound - getLocalDateTime(upperBound).getSecond() % 15;
                unit = 15;
            } else if (tickSeparation == TickSeparation.THIRTY_SECONDS) {
                lastValue = upperBound - getLocalDateTime(upperBound).getSecond() % 30;
                unit = 30;
            } else if (tickSeparation == TickSeparation.MINUTE) {
                lastValue = upperBound - getLocalDateTime(upperBound).getSecond();
                unit = 60;
            } else if (tickSeparation == TickSeparation.FIVE_MINUTES) {
                LocalDateTime roundedLastTime = getLocalDateTime(upperBound).withSecond(0);
                lastValue = getEpochSeconds(roundedLastTime) - 60*(roundedLastTime.getMinute() % 5);
                unit = 60*5;
            } else if (tickSeparation == TickSeparation.FIFTEEN_MINUTES) {
                LocalDateTime roundedLastTime = getLocalDateTime(upperBound).withSecond(0);
                lastValue = getEpochSeconds(roundedLastTime) - 60*(roundedLastTime.getMinute() % 15);
                unit = 60*15;
            } else if (tickSeparation == TickSeparation.THIRTY_MINUTES) {
                LocalDateTime roundedLastTime = getLocalDateTime(upperBound).withSecond(0);
                lastValue = getEpochSeconds(roundedLastTime) - 60*(roundedLastTime.getMinute() % 30);
                unit = 60*30;
            } else if (tickSeparation == TickSeparation.HOUR) {
                lastValue = getEpochSeconds(getLocalDateTime(upperBound).withSecond(0).withMinute(0));
                unit = 3600;
            } else if (tickSeparation == TickSeparation.THREE_HOURS) {
                LocalDateTime roundedLastTime = getLocalDateTime(upperBound).withSecond(0).withMinute(0);
                lastValue = getEpochSeconds(roundedLastTime) - 3600*(roundedLastTime.getHour() % 3);
                unit = 3600*3;
            } else if (tickSeparation == TickSeparation.SIX_HOURS) {
                LocalDateTime roundedLastTime = getLocalDateTime(upperBound).withSecond(0).withMinute(0);
                lastValue = getEpochSeconds(roundedLastTime) - 3600*(roundedLastTime.getHour() % 6);
                unit = 3600*6;
            } else if (tickSeparation == TickSeparation.TWELVE_HOURS) {
                LocalDateTime roundedLastTime = getLocalDateTime(upperBound).withSecond(0).withMinute(0);
                lastValue = getEpochSeconds(roundedLastTime) - 3600*(roundedLastTime.getHour() % 12);
                unit = 3600*12;
            } else if (tickSeparation == TickSeparation.DAY) {
                lastValue = getEpochSeconds(getLocalDateTime(upperBound).withSecond(0).withMinute(0).withHour(0));
                unit = 86400;
            } else if (tickSeparation == TickSeparation.WEEK) {
                LocalDateTime roundedLastTime = getLocalDateTime(upperBound).withSecond(0).withMinute(0).withHour(0);
                int firstDayOfWeek = Utils.getOrDefault(getFirstDayOfWeek(), DayOfWeek.MONDAY).getValue();  // 1-7
                // offset = (7 + day - firstDay) % 7
                lastValue = getEpochSeconds(roundedLastTime) - 86400*((roundedLastTime.getDayOfWeek().getValue()+7-firstDayOfWeek) % 7);
                unit = 86400*7;
            } else {
                throw new AssertionError("Can't reach this");
            }
            for (long i = lastValue; i >= lowerBound; i -= unit) {
                tickValues.add(0, i);   // add to start of list, so it is ascending order
            }
        } else {
            if (tickSeparation == TickSeparation.MONTH) {
                LocalDateTime roundedLastTime = getLocalDateTime(upperBound).withSecond(0).withMinute(0).withHour(0).withDayOfMonth(1);
                while (getEpochSeconds(roundedLastTime) >= lowerBound) {
                    tickValues.add(0, getEpochSeconds(roundedLastTime));   // add to start of list, so it is ascending order
                    roundedLastTime = roundedLastTime.minusMonths(1);
                }
            } else if (tickSeparation == TickSeparation.THREE_MONTHS) {
                LocalDateTime roundedLastTime = getLocalDateTime(upperBound).withSecond(0).withMinute(0).withHour(0).withDayOfMonth(1);
                roundedLastTime = roundedLastTime.minusMonths((roundedLastTime.getMonthValue() - 1) % 3);
                while (getEpochSeconds(roundedLastTime) >= lowerBound) {
                    tickValues.add(0, getEpochSeconds(roundedLastTime));
                    roundedLastTime = roundedLastTime.minusMonths(3);
                }
            } else if (tickSeparation == TickSeparation.SIX_MONTHS) {
                LocalDateTime roundedLastTime = getLocalDateTime(upperBound).withSecond(0).withMinute(0).withHour(0).withDayOfMonth(1);
                roundedLastTime = roundedLastTime.minusMonths((roundedLastTime.getMonthValue() - 1) % 6);
                while (getEpochSeconds(roundedLastTime) >= lowerBound) {
                    tickValues.add(0, getEpochSeconds(roundedLastTime));
                    roundedLastTime = roundedLastTime.minusMonths(6);
                }
            } else if (tickSeparation == TickSeparation.YEAR) {
                LocalDateTime roundedLastTime = getLocalDateTime(upperBound).withSecond(0).withMinute(0).withHour(0).withDayOfYear(1);
                while (getEpochSeconds(roundedLastTime) >= lowerBound) {
                    tickValues.add(0, getEpochSeconds(roundedLastTime));
                    roundedLastTime = roundedLastTime.minusYears(1);
                }
            } else {
                throw new AssertionError("Can't reach this");
            }
        }
//        System.out.println(tickSeparation);
        return tickValues;
    }

    // -------------- PROTECTED METHODS --------------------------------------------------------------------------------

    @Override
    protected String getTickMarkLabel(Long value) {
        LocalDateTime dateTime = getLocalDateTime(value);
        DateTimeFormatter formatter;
        if (dateTime.getSecond() == 0 && dateTime.getMinute() == 0) {
            if (dateTime.getHour() == 0) {
                if (dateTime.getDayOfMonth() == 1) {
                    if (dateTime.getMonth() == Month.JANUARY) {
                        formatter = Utils.getOrDefault(getYearFormatter(), DEFAULT_YEAR_FORMATTER);
                    } else {
                        formatter = Utils.getOrDefault(getMonthFormatter(), DEFAULT_MONTH_FORMATTER);
                    }
                } else {    // day
                    formatter = Utils.getOrDefault(getDayFormatter(), DEFAULT_DAY_FORMATTER);
                }
            } else {    // hour
                formatter = Utils.getOrDefault(getHourFormatter(), DEFAULT_HOUR_FORMATTER);
            }
        } else {    // time
            formatter = Utils.getOrDefault(getTimeFormatter(), DEFAULT_TIME_FORMATTER);
        }
//        System.out.println(formatter.format(dateTime));
        return formatter.format(dateTime);
    }

    /**
     * Called to get the current axis range.
     * @return A range object that can be passed to setRange() and calculateTickValues().
     */
    @Override
    protected Object getRange() {
        return new Object[] {getLowerBound(), getUpperBound(), getMaxTickMarks(), getScale()};
    }

    @Override
    protected Object autoRange(double minValue, double maxValue, double length, double labelSize) {
        labelSize = labelSize * 2;    // approx label size
        double valueRange = maxValue - minValue;
        LocalDateTime minDateTime = getLocalDateTime((long) minValue);
        LocalDateTime roundedMinDateTime;

        if (valueRange >= 86400*365*10) {   // >= 10 years
            roundedMinDateTime = minDateTime.withSecond(0).withMinute(0).withHour(0).withDayOfYear(1);
        } else if (valueRange >= 86400*28*9) { // >= 9 months
            roundedMinDateTime = minDateTime.withSecond(0).withMinute(0).withHour(0).withDayOfMonth(1);
        } else if (valueRange >= 86400*10) { // >= 10 days
            roundedMinDateTime = minDateTime.withSecond(0).withMinute(0).withHour(0);
        } else if (valueRange >= 3600*10) { // >= 10 hours
            roundedMinDateTime = minDateTime.withSecond(0).withMinute(0);
        } else if (valueRange >= 60*15) {   // >= 15 minutes
            roundedMinDateTime = minDateTime.withSecond(0);
        } else {
            roundedMinDateTime = minDateTime;
        }
        double roundedLowerBound = getEpochSeconds(roundedMinDateTime);
        int maxTicksCount = (int) Math.ceil((length / labelSize)*0.8);
        final double newScale = calculateNewScale(length, roundedLowerBound, maxValue);
//        System.out.println("max ticks: " + maxTicksCount);
        return new Object[] {roundedLowerBound, maxValue, maxTicksCount, newScale};
    }

    @Override
    protected List<Long> calculateTickValues(double length, Object range) {
        final Object[] rangeItems = (Object[]) range;
        final double lowerBound = (double) rangeItems[0];
        final double upperBound = (double) rangeItems[1];
        final int maxTickMarks = (int) rangeItems[2];
        List<Long> tickValues = new ArrayList<>();
        if (lowerBound == upperBound || maxTickMarks <= 1) {
            tickValues.add((long) upperBound);
        } else if (lowerBound > upperBound || maxTickMarks == 2) {
            tickValues.add((long) lowerBound);
            tickValues.add((long) upperBound);
        } else {
            long valueRange = (long) (upperBound - lowerBound);
            long minTickRange = valueRange / maxTickMarks;
            if (minTickRange >= 86400*182) {
                tickSeparation.set(TickSeparation.YEAR);
            } else if (minTickRange >= 86400*90) {
                tickSeparation.set(TickSeparation.SIX_MONTHS);
            } else if (minTickRange >= 86400*28) {
                tickSeparation.set(TickSeparation.THREE_MONTHS);
            } else if (minTickRange >= 86400*7) {
                tickSeparation.set(TickSeparation.MONTH);
            } else if (minTickRange >= 86400) {
                tickSeparation.set(TickSeparation.WEEK);
            } else if (minTickRange >= 3600*12) {
                tickSeparation.set(TickSeparation.DAY);
            } else if (minTickRange >= 3600*6) {
                tickSeparation.set(TickSeparation.TWELVE_HOURS);
            } else if (minTickRange >= 3600*3) {
                tickSeparation.set(TickSeparation.SIX_HOURS);
            } else if (minTickRange >= 3600) {
                tickSeparation.set(TickSeparation.THREE_HOURS);
            } else if (minTickRange >= 60*30) {
                tickSeparation.set(TickSeparation.HOUR);
            } else if (minTickRange >= 60*15) {
                tickSeparation.set(TickSeparation.THIRTY_MINUTES);
            } else if (minTickRange >= 60*5) {
                tickSeparation.set(TickSeparation.FIFTEEN_MINUTES);
            } else if (minTickRange >= 60) {
                tickSeparation.set(TickSeparation.FIVE_MINUTES);
            } else if (minTickRange >= 30) {
                tickSeparation.set(TickSeparation.MINUTE);
            } else if (minTickRange >= 15) {
                tickSeparation.set(TickSeparation.THIRTY_SECONDS);
            } else if (minTickRange >= 5) {
                tickSeparation.set(TickSeparation.FIFTEEN_SECONDS);
            } else if (minTickRange >= 1) {
                tickSeparation.set(TickSeparation.FIVE_SECONDS);
            } else {
                tickSeparation.set(TickSeparation.SECOND);
            }
            return calculateTickValues((long) lowerBound, (long) upperBound, tickSeparation.get());
        }
        return tickValues;
    }

    /**
     * No minor tick marks.
     * @return An empty list
     */
    @Override
    protected List<Long> calculateMinorTickMarks() {
        return new ArrayList<>();
    }

    @Override
    protected void setRange(Object range, boolean animate) {
        final Object[] rangeItems = (Object[]) range;
        final double lowerBound = (double) rangeItems[0];
        final double upperBound = (double) rangeItems[1];
        final int maxTickMarks = (int) rangeItems[2];
        final double scale = (double) rangeItems[3];
        setLowerBound(lowerBound);
        setUpperBound(upperBound);
        setMaxTickMarks(maxTickMarks);
        currentLowerBound.set(lowerBound);
        setScale(scale);
    }

    // -------------- METHODS ------------------------------------------------------------------------------------------

    @Override
    public Long toRealValue(double value) {
        return (long) value;
    }

    // -------------- STYLESHEET HANDLING ------------------------------------------------------------------------------




    public enum TickSeparation {
        YEAR, SIX_MONTHS, THREE_MONTHS, MONTH, WEEK, DAY, TWELVE_HOURS, SIX_HOURS, THREE_HOURS, HOUR,
        THIRTY_MINUTES, FIFTEEN_MINUTES, FIVE_MINUTES, MINUTE, THIRTY_SECONDS, FIFTEEN_SECONDS, FIVE_SECONDS, SECOND;

        public static boolean isConsistent(TickSeparation tickSeparation) {
            switch (tickSeparation) {
                case SECOND:
                case FIVE_SECONDS:
                case FIFTEEN_SECONDS:
                case THIRTY_SECONDS:
                case MINUTE:
                case FIVE_MINUTES:
                case FIFTEEN_MINUTES:
                case THIRTY_MINUTES:
                case HOUR:
                case THREE_HOURS:
                case SIX_HOURS:
                case TWELVE_HOURS:
                case DAY:
                case WEEK:
                    return true;
            }
            return false;
        }
    }
}
