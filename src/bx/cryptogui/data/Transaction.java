package bx.cryptogui.data;

public class Transaction extends Trade {

    public static final int BUY = 0;
    public static final int SELL = 1;

    private final int tradeType;

    public Transaction(long id, long time, double price, double volume, int type, TradePlatform tradePlatform) {
        super(id, time, price, volume, tradePlatform);
        this.tradeType = type;
    }

//    public Transaction(long time, double price, double volume, int type, TradePlatform tradePlatform) {
//        this(-1L, time, price, volume, type, tradePlatform);
//    }

    public final int getTradeType() {
        return tradeType;
    }

    @Override
    public String toString() {
        return String.format("TradeData[%s,%s,%s,%s,%s]",
                getTime(), getPrice(), getVolume(), getTradeTypeString(tradeType), getTradePlatform());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Transaction && super.equals(obj) && tradeType == ((Transaction) obj).tradeType;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + tradeType*4;
    }

    public static String getTradeTypeString(int tradeType) {
        if (tradeType == 0) {
            return "BUY";
        } else if (tradeType == 1) {
            return "SELL";
        } else {
            return "UNKNOWN";
        }
    }
}
