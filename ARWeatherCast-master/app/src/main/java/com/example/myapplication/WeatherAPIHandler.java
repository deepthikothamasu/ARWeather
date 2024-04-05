package com.example.myapplication;
import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class WeatherAPIHandler {

    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/forecast";
    private static final String API_KEY = "c8f0410d68725b537877582a39952f9d";

    private final RequestQueue requestQueue;

    public WeatherAPIHandler(Context context) {
        requestQueue = Volley.newRequestQueue(context);
    }

    public void getWeatherData(String cityName, final WeatherDataCallback callback) {
        String url = String.format("%s?q=%s&appid=%s", BASE_URL, cityName, API_KEY);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> callback.onSuccess(response),
                error -> callback.onError(error));

        requestQueue.add(request);
    }

    public interface WeatherDataCallback {
        void onSuccess(JSONObject response);
        void onError(VolleyError error);
    }
}
