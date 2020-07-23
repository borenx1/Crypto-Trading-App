package bx.cryptogui.exchangeapi;

public class HTTPException extends Exception {

    public HTTPException(int code, String message) {
        super(code + ", " + message);
    }

    public HTTPException(HTTPResponse response) {
        this(response.getResponseCode(), response.getResponseMessage());
    }
}
