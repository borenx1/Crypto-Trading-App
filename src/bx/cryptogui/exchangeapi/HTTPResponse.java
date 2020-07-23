package bx.cryptogui.exchangeapi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class HTTPResponse {

    private final int responseCode;
    private final String responseMessage;
    private final Map<String, List<String>> header;
    private final String body;

    public HTTPResponse(Map<String, List<String>> header, String body, int responseCode, String responseMessage) {
        this.header = header;
        this.body = body;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
    }

    public final Map<String, List<String>> getHeaderFields() {
        return header;
    }

    public final List<String> getHeaderField(String key) {
        return header.get(key);
    }

    public final String getBody() {
        return body;
    }

    public final int getResponseCode() {
        return responseCode;
    }

    public final String getResponseMessage() {
        return responseMessage;
    }

    public JSONObject getJSONObject() {
        return new JSONObject(body);
    }

    public JSONArray getJSONArray() {
        return new JSONArray(body);
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)\n%s", responseCode, responseMessage, body);
    }
}
