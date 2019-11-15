package com.pushdy.core.network;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.google.gson.Gson;
import com.google.gson.JsonElement;


/**
 * A request for retrieving a {@link JsonElement} response body at a given URL, allowing for an
 * optional {@link JsonElement} to be passed in as part of the request body.
 */
public class PDYJsonRequest extends JsonRequest<JsonElement> {

    /**
     * Creates a new request.
     *
     * @param method the HTTP method to use
     * @param url URL to fetch the JSON from
     * @param jsonRequest A {@link JsonElement} to post with the request. Null indicates no
     *     parameters will be posted along with request.
     * @param listener Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public PDYJsonRequest(
            int method,
            String url,
            JsonElement jsonRequest,
            Response.Listener<JsonElement> listener,
            Response.ErrorListener errorListener) {
        super(
                method,
                url,
                (jsonRequest == null) ? null : jsonRequest.toString(),
                listener,
                errorListener);
    }

    @Override
    protected Response<JsonElement> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString =
                    new String(
                            response.data,
                            HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
            Gson gson = new Gson();
            JsonElement jsonElement = gson.fromJson(jsonString, JsonElement.class);
            return Response.success(
                    jsonElement, HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            return Response.error(new ParseError(e));
        }
    }
}
