package bx.cryptogui.data;

public class Ticker {

    private final double last;
    private final int type;
    private final double high;
    private final double low;
    private final double average;
    private final double volume;
    private final long time;

    public Ticker(long time, double last, int type, double high, double low, double average, double volume) {
        this.time = time;
        this.last = last;
        this.type = type;
        this.high = high;
        this.low = low;
        this.average = average;
        this.volume = volume;
    }

    public final long getTime() {
        return time;
    }

    public final double getLast() {
        return last;
    }

    /**
     * @return 0 for buy/ask (higher), 1 for sell/bid (lower), -1 for other
     */
    public final int getType() {
        return type;
    }

    public final double getHigh() {
        return high;
    }

    public final double getLow() {
        return low;
    }

    public final double getAverage() {
        return average;
    }

    public final double getVolume() {
        return volume;
    }

    @Override
    public String toString() {
        return String.format("Ticker[time=%s, last=%s, high=%s, low=%s, average=%s, volume=%s]",
                time, last, high, low, average, volume);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Ticker) {
            Ticker o = (Ticker) obj;
            return time == o.time && last == o.last && high == o.high && low == o.low &&
                    average == o.average && volume == o.volume && type == o.type;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) (time - last + high*low + average + volume) >> type;
    }
}


