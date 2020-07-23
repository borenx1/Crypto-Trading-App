package bx.cryptogui.data;

import java.util.Objects;

public class Trade implements Comparable<Trade> {

    private final long id;
    private final long time;
    private final double price;
    private final double volume;
    private final TradePlatform tradePlatform;

    public Trade(long id, long time, double price, double volume, TradePlatform tradePlatform) {
        this.id = id;
        this.time = time;
        this.price = price;
        this.volume = volume;
        this.tradePlatform = tradePlatform;
    }

    public final long getId() {
        return id;
    }

    public final long getTime() {
        return time;
    }

    public final double getPrice() {
        return price;
    }

    public final double getVolume() {
        return volume;
    }

    public final double getQuoteVolume() {
        return volume*price;
    }

    public final TradePlatform getTradePlatform() {
        return tradePlatform;
    }

    @Override
    public String toString() {
        return String.format("Trade[%s,%s,%s,%s]", time, price, volume, tradePlatform);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Trade) {
            Trade other = (Trade) obj;
            return time == other.time && price == other.price &&
                    volume == other.volume && Objects.equals(tradePlatform, other.tradePlatform);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (int) (time - ((int) price)*3 + volume*11 + Objects.hash(tradePlatform));
    }

    @Override
    public int compareTo(Trade o) {
        return Long.compare(time, o.time);
    }
}
