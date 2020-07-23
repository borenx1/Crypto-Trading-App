package bx.cryptogui.exchangeapi;

public class AuthorisationInvalidException extends RequestException {

    public AuthorisationInvalidException() {
        super();
    }

    public AuthorisationInvalidException(String message) {
        super(message);
    }

    public AuthorisationInvalidException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthorisationInvalidException(Throwable cause) {
        super(cause);
    }
}
