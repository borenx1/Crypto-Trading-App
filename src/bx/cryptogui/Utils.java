package bx.cryptogui;


import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class Utils {

    public static final ZoneOffset zoneOffset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    public static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Utils() {}

    public static boolean between(double i, double min, double max) {
        return i > min && i < max;
    }

    public static boolean betweenInclusive(double i, double min, double max) {
        return i >= min && i <= max;
    }

    public static <T> T getOrDefault(T obj, T defaultValue) {
        return obj == null ? defaultValue : obj;
    }

    public static long getClosestValue(long num, List<Long> vals) {
        if (vals.isEmpty() || vals.contains(num)) {
            return num;
        }
        long closest = vals.get(0);
        long smallestGap = Long.MAX_VALUE;
        for (long val: vals) {
            long gap = Math.abs(num - val);
            if (gap < smallestGap) {
                closest = val;
                smallestGap = gap;
            }
        }
        return closest;
    }

    public static double roundSigFig(double num, int sigFig) {
        BigDecimal dec = BigDecimal.valueOf(num);
        return dec.setScale(sigFig + dec.scale() - dec.precision(), BigDecimal.ROUND_HALF_UP).stripTrailingZeros().doubleValue();
    }

    public static String formatDecimal(double num, int sigFig, boolean commas) {
        char[] hashes = new char[sigFig];
        Arrays.fill(hashes, '#');
        DecimalFormat format = commas ? new DecimalFormat("###,###." + new String(hashes)) : new DecimalFormat("###." + new String(hashes));
        return format.format(roundSigFig(num, sigFig));
    }

    // --------------------- DATE/TIME ---------------------------------------------------------------------------------

    public static ZoneOffset getLocalZoneOffset() {
        return zoneOffset;
    }

    public static LocalDateTime getLocalDateTime(long epochSeconds) {
        return LocalDateTime.ofEpochSecond(epochSeconds, 0, zoneOffset);
    }

    public static long getEpochSeconds(LocalDateTime dateTime) {
        return dateTime.toEpochSecond(zoneOffset);
    }

    /**
     * Formats to HH:mm:ss.
     * @param epochSeconds seconds since epoch
     * @return formatted string
     */
    public static String formatTime(long epochSeconds) {
        return formatTime(getLocalDateTime(epochSeconds));
    }

    public static String formatTime(LocalDateTime dateTime) {
        return timeFormatter.format(dateTime);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTimeFormatter.format(dateTime);
    }

    public static String formatDateTime(long epochSeconds) {
        return formatDateTime(getLocalDateTime(epochSeconds));
    }


    public static long snapToPreviousSecond(long time, int seconds) {
        if (seconds <= 0) {
            throw new IllegalArgumentException("Seconds must be positive");
        }
        if (seconds < 60) {
            return time - LocalDateTime.ofEpochSecond(time, 0, zoneOffset).getSecond() % seconds;
        }
        double minutes = seconds/60.0;
        if (Math.rint(minutes) == minutes) {
            return snapToPreviousMinute(time, (int) minutes);
        }
        throw new IllegalArgumentException("Seconds is invalid: " + seconds);
    }

    public static long snapToPreviousMinute(long time, int minutes) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("Minutes must be positive");
        }
        if (minutes < 60) {
            LocalDateTime roundedTime = LocalDateTime.ofEpochSecond(time, 0, zoneOffset).withSecond(0);
            return roundedTime.toEpochSecond(zoneOffset) - 60*(roundedTime.getMinute() % minutes);
        }
        double hours = minutes/60.0;
        if (Math.rint(hours) == hours) {
            return snapToPreviousHour(time, (int) hours);
        }
        throw new IllegalArgumentException("Minutes is invalid: " + minutes);
    }

    public static long snapToPreviousHour(long time, int hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("Hours must be positive");
        }
        if (hours < 24) {
            LocalDateTime roundedTime = LocalDateTime.ofEpochSecond(time, 0, zoneOffset)
                    .withSecond(0).withMinute(0);
            return roundedTime.toEpochSecond(zoneOffset) - 3600*(roundedTime.getHour() % hours);
        }
        double days = hours/24.0;
        if (Math.rint(days) == days) {
            return snapToPreviousDay(time, (int) days);
        }
        throw new IllegalArgumentException("Hours is invalid: " + hours);
    }

    public static long snapToPreviousDay(long time, int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be positive");
        }
        long daysSinceEpoch = time / 86400;
        LocalDateTime roundedTime = LocalDateTime.ofEpochSecond(time, 0, zoneOffset)
                .withSecond(0).withMinute(0).withHour(0);
        return roundedTime.toEpochSecond(zoneOffset) - 86400*(daysSinceEpoch % days);
    }

    public static long snapToPreviousWeek(long time, int weeks, DayOfWeek startWeek) {
        LocalDateTime roundedLastTime = LocalDateTime.ofEpochSecond(time, 0, zoneOffset)
                .withSecond(0).withMinute(0).withHour(0);
        int firstDayOfWeek = startWeek.getValue();  // 1-7
        // offset = (7 + day - firstDay) % 7
        return roundedLastTime.toEpochSecond(zoneOffset) - 86400*((roundedLastTime.getDayOfWeek().getValue()+7-firstDayOfWeek) % 7);
    }
}
