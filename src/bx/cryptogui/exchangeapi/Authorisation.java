package bx.cryptogui.exchangeapi;

import java.util.Objects;

public class Authorisation {

    private final String key;
    private final String secret;

    public Authorisation(String key, String secret) throws NullPointerException, IllegalArgumentException {
        this.key = Objects.requireNonNull(key);
        this.secret = Objects.requireNonNull(secret);
        if (key.isEmpty() || secret.isEmpty()) {
            throw new IllegalArgumentException("Key or secret is empty");
        }
    }

    public final String getKey() {
        return key;
    }

    public final String getSecret() {
        return secret;
    }

    @Override
    public String toString() {
        return "Account.Authorisation";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Authorisation) {
            Authorisation o = (Authorisation) obj;
            return key.equals(o.key) && secret.equals(o.secret);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, secret) >> 2;
    }
}
