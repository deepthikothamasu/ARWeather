package com.example.myapplication;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class WeatherWorker extends Worker {

    private Context context;

    public WeatherWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        // Fetch weather data
        if (!isInitialNotificationSent()) {
            // Send the initial notification
            sendInitialNotification();
            setInitialNotificationSent(true); // Set the flag to indicate the initial notification has been sent
        } else {
            // Send periodic notifications
            String cityName = getCityName();
            getWeatherInfo(cityName);
        }
        String cityName = getCityName(); // Fetch the city name
        getWeatherInfo(cityName);
        return Result.success();
    }
    private void sendInitialNotification() {
        if (!isInitialNotificationSent()) {
            // Build and display the initial notification
            createNotificationChannel();
            try {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "weather_notification")
                        .setSmallIcon(R.drawable.baseline_notifications_active_24)
                        .setContentTitle("Welcome to Weather App")
                        .setContentText("You will now receive weather updates every 5 minutes.")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(1, builder.build());

                // Mark the initial notification as sent
                setInitialNotificationSent(true);
            } catch (SecurityException e) {
                Log.e("WeatherWorker", "SecurityException: " + e.getMessage());
            }
        }
    }

    private boolean isInitialNotificationSent() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("WeatherWorkerPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean("InitialNotificationSent", false);
    }

    private void setInitialNotificationSent(boolean sent) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("WeatherWorkerPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("InitialNotificationSent", sent);
        editor.apply();
    }


    private String getCityName() {
        String cityName = ""; // Default to an empty string
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, proceed to get city name
            try {
                android.location.LocationManager locationManager = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                android.location.Location location = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    Geocoder gcd = new Geocoder(context, Locale.getDefault());
                    List<Address> addresses = gcd.getFromLocation(latitude, longitude, 1);
                    if (addresses.size() > 0) {
                        cityName = addresses.get(0).getLocality();
                    }
                }
            } catch (SecurityException e) {
                // Handle SecurityException
                Log.e("WeatherWorker", "Location permission denied: " + e.getMessage());
            } catch (IOException e) {
                // Handle IOException
                Log.e("WeatherWorker", "IOException: " + e.getMessage());
            }
        } else {
            // Permission not granted, handle accordingly
            Log.e("WeatherWorker", "Location permission not granted");
        }
        return cityName;
    }

    private void sendNotification(String cityName, String temperature) {
        createNotificationChannel();
        try {
            // Check if the app has the required notification permission
            if (hasNotificationPermission()) {
                // Build the notification
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "weather_notification")
                        .setSmallIcon(R.drawable.baseline_notifications_active_24)
                        .setContentTitle("Weather Update")
                        .setContentText("Current temperature in " + cityName + " is " + temperature + "°C")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                // Display the notification
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(1, builder.build());
            } else {
                Log.e("WeatherWorker", "Notification permission not granted");
            }
        } catch (SecurityException e) {
            // Handle SecurityException
            Log.e("WeatherWorker", "SecurityException: " + e.getMessage());
        }
    }

    private void getWeatherInfo(String cityName) {
        String url = "https://api.weatherapi.com/v1/forecast.json?key=242ceaef0654493098e55318231512&q=" + cityName + "&days=1&aqi=yes&alerts=yes";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                // Parse the JSON response and update UI with weather information
                try {
                    String temperature = response.getJSONObject("current").getString("temp_c");
                    sendNotification(cityName, temperature);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("WeatherApp", "Error: " + error.toString());
                // Handle error
            }
        });

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(jsonObjectRequest);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "WeatherChannel";
            String description = "Channel for Weather Notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("weather_notification", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.checkSelfPermission(Manifest.permission.ACCESS_NOTIFICATION_POLICY)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // If SDK version is below M, assume permission is granted
    }

}