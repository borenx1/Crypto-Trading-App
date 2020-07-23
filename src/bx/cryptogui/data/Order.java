package bx.cryptogui.data;

public class Order extends Trade {

    private final boolean bid;

    public Order(long id, long time, double price, double volume, boolean bid, TradePlatform tradePlatform) {
        super(id, time, price, volume, tradePlatform);
        this.bid = bid;
    }

    public final boolean isBid() {
        return bid;
    }

    public final boolean isAsk() {
        return !bid;
    }

    @Override
    public String toString() {
        return String.format("Order[%s,%s,%s,%s,%s]",
                getTime(), getPrice(), getVolume(), bid ? "BID" : "ASK", getTradePlatform());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Order && super.equals(obj) && bid == ((Order) obj).bid;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + (bid ? 11 : -13);
    }
}
