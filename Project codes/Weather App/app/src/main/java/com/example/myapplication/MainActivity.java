package com.example.myapplication;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements MyFirebaseMessagingService.PermissionHandler {

    DrawerLayout drawerLayout;
    ImageView menu;
    LinearLayout home, settings, share, about, logout;

    private RelativeLayout homeRL;
    private ProgressBar loadingPB;
    private TextView cityNameTV, temperatureTV, conditionTV, signup_user;
    private RecyclerView weatherRV;
    private TextInputEditText cityEdt;
    private ImageView backIV, iconIV, searchIV;
    private ArrayList<WeatherRVModal> weatherRVModelArrayList;
    private WeatherRVAdapter weatherRVAdapter;
    private LocationManager locationManager;
    private int PERMISSION_CODE = 1;
    private String cityName;
    private TextView userEmailTextView;
    private FirebaseAuth auth;
    private String selectedTemperatureMetric;
    private String selectedWindSpeedMetric;
    private DatabaseReference bookmarksRef;
    private ValueEventListener bookmarksListener;
    private ImageView bookmarkButton;
    private WeatherDatabaseHelper dbHelper;

    private List<String> bookmarksList = new ArrayList<>();

    // Define locationListener outside onCreate
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            Log.d("Location", "Latitude: " + latitude + ", Longitude: " + longitude);

            cityName = getCityName(longitude, latitude);
            getWeatherInfo(cityName);


            // Don't forget to remove updates if not needed anymore
            locationManager.removeUpdates(this);
        }
    };
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        setContentView(R.layout.activity_main);
        scheduleWeatherUpdates();
        cityNameTV = findViewById(R.id.idTVCityName);
        temperatureTV = findViewById(R.id.idTVTemperature);
        conditionTV = findViewById(R.id.idTVCondition);
        cityNameTV.setText("Guntur");
        temperatureTV.setText("23 C");
        conditionTV.setText("Weather Info");

        dbHelper = new WeatherDatabaseHelper(this);
        if (!isNetworkAvailable()) {
            // Load weather data from the database
            queryDataFromDatabase();
            // You may also want to notify the user that they are viewing cached data
            Toast.makeText(MainActivity.this, "You are viewing cached weather data.", Toast.LENGTH_SHORT).show();
        }



        FirebaseMessaging.getInstance().subscribeToTopic("weather")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "Done";
                        if (!task.isSuccessful()) {
                            msg = "Failed";
                        }
                    }
                });



        SharedPreferences sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE);
        String selectedAppTheme = sharedPreferences.getString("appTheme", "Light");
        selectedWindSpeedMetric = sharedPreferences.getString("windSpeedMetric", "km/h");

        selectedTemperatureMetric = sharedPreferences.getString("temperatureMetric", "Celsius");

        // Apply selected theme
        if (selectedAppTheme.equals("Light")) {
            setTheme(R.style.LightTheme);
        } else if (selectedAppTheme.equals("Dark")) {
            setTheme(R.style.DarkTheme);
        }
        drawerLayout = findViewById(R.id.drawerLayout);
        menu = findViewById(R.id.menu);
        home = findViewById(R.id.home);
        about = findViewById(R.id.about);
        logout = findViewById(R.id.logout);
        settings = findViewById(R.id.settings);
        share = findViewById(R.id.share);
        auth = FirebaseAuth.getInstance();
        userEmailTextView = findViewById(R.id.userEmailTextView);
        signup_user = findViewById(R.id.signup_user);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // User is signed in, set the username
            String username = currentUser.getDisplayName();
            signup_user.setText(username);
        } else {
            signup_user.setText("Unknown User");
        }
        if (currentUser != null) {
            String userId = currentUser.getUid();
            bookmarksRef = FirebaseDatabase.getInstance().getReference().child("bookmarks").child(userId);
            loadBookmarks();
        }

        bookmarkButton = findViewById(R.id.bookmark_btn);
        // Add the OnClickListener for the bookmark button
        bookmarkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cityName = cityNameTV.getText().toString();
                if (bookmarksList.contains(cityName)) {
                    // City is already bookmarked, so remove it
                    bookmarksRef.child(cityName).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(MainActivity.this, "City removed from bookmarks", Toast.LENGTH_SHORT).show();
                                bookmarksList.remove(cityName);
                                updateBookmarkButton();
                            } else {
                                Toast.makeText(MainActivity.this, "Failed to remove city from bookmarks", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    // City is not bookmarked, so add it
                    bookmarksRef.child(cityName).setValue(true).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(MainActivity.this, "City added to bookmarks", Toast.LENGTH_SHORT).show();
                                bookmarksList.add(cityName);
                                updateBookmarkButton();
                            } else {
                                Toast.makeText(MainActivity.this, "Failed to add city to bookmarks", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });


        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDrawer(drawerLayout);
            }
        });
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recreate();
            }
        });
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redirectActivity(MainActivity.this, SettingsActivity.class);

            }
        });
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //redirectActivity(MainActivity.this, ShareActivity.class);
                String cityName = cityNameTV.getText().toString();
                Intent intent = new Intent(MainActivity.this, ShareActivity.class);
                intent.putExtra("cityName", cityName);
                startActivity(intent);


            }
        });
        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redirectActivity(MainActivity.this, AboutActivity.class);

            }
        });
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                auth.signOut();

                // Redirect the user to the login screen or any other appropriate screen
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();

            }
        });
        //NotificationScheduler.scheduleHourlyNotifications(getApplicationContext());



        homeRL = findViewById(R.id.idRLHome);
        loadingPB = findViewById(R.id.idPBLoading);
        cityNameTV = findViewById(R.id.idTVCityName);
        temperatureTV = findViewById(R.id.idTVTemperature);
        conditionTV = findViewById(R.id.idTVCondition);
        weatherRV = findViewById(R.id.idRvWeather);
        cityEdt = findViewById(R.id.idEdtCity);
        backIV = findViewById(R.id.idIVBack);
        iconIV = findViewById(R.id.idIVIcon);
        searchIV = findViewById(R.id.idIVSearch);
        weatherRVModelArrayList = new ArrayList<>();
        weatherRVAdapter = new WeatherRVAdapter(this, weatherRVModelArrayList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        weatherRV.setLayoutManager(layoutManager);
        weatherRV.setAdapter(weatherRVAdapter);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        cityNameTV.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed for your implementation
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed for your implementation
            }

            @Override
            public void afterTextChanged(Editable s) {
                loadBookmarks();
            }
        });


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_CODE
            );
        } else {
            // Request location updates
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0,
                    locationListener // <-- Use the locationListener defined outside
            );

            // Get the last known location
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                cityName = getCityName(location.getLongitude(), location.getLatitude());
                getWeatherInfo(cityName);
            } else {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
            }
        }

        searchIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = cityEdt.getText().toString();
                if (city.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter city Name", Toast.LENGTH_SHORT).show();
                } else {
                    cityNameTV.setText(cityName);
                    getWeatherInfo(city);
                }
            }
        });
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("cityName")) {
            String cityName = intent.getStringExtra("cityName");
            // Now you have the cityName, you can load weather information for this city
            getWeatherInfo(cityName);
        }
    }
    private void scheduleWeatherUpdates() {
        createNotificationChannel();
        // Define the initial one-time work request to trigger immediately
        OneTimeWorkRequest initialWorkRequest = new OneTimeWorkRequest.Builder(WeatherWorker.class)
                .build();

        // Enqueue the initial one-time work request
        WorkManager.getInstance().enqueue(initialWorkRequest);

        // Define the periodic work request to trigger every 2 HOURS
        PeriodicWorkRequest periodicWorkRequest =
                new PeriodicWorkRequest.Builder(WeatherWorker.class, 2, TimeUnit.HOURS)
                        .build();

        // Enqueue the periodic work request
        WorkManager.getInstance().enqueue(periodicWorkRequest);
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "WeatherChannel";
            String description = "Channel for Weather Notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("weather_notification", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private void insertDataIntoDatabase(String cityName, String temperature, String windSpeed, String condition) {
        // Open the database
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Insert data into the database
        ContentValues values = new ContentValues();
        values.put(WeatherDatabaseHelper.COLUMN_CITY_NAME, cityName);
        values.put(WeatherDatabaseHelper.COLUMN_TEMPERATURE, temperature);
        values.put(WeatherDatabaseHelper.COLUMN_WIND_SPEED, windSpeed);
        values.put(WeatherDatabaseHelper.COLUMN_CONDITION, condition);
        // Add more values as needed
        long newRowId = db.insert(WeatherDatabaseHelper.TABLE_WEATHER, null, values);

        // Close the database
        db.close();
    }

    // Example method to query data from the database
    private void queryDataFromDatabase() {
        // Open the database in read mode
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Define a projection that specifies the columns from the table you care about
        String[] projection = {
                WeatherDatabaseHelper.COLUMN_CITY_NAME,
                WeatherDatabaseHelper.COLUMN_TEMPERATURE,
                WeatherDatabaseHelper.COLUMN_WIND_SPEED,
                WeatherDatabaseHelper.COLUMN_CONDITION
                // Add more columns if needed
        };

        // Define selection criteria if necessary
        String selection = null;
        String[] selectionArgs = null;

        // Define how the results should be sorted
        String sortOrder =
                WeatherDatabaseHelper.COLUMN_CITY_NAME + " DESC"; // Example sorting by city name in descending order

        // Perform a query on the database
        Cursor cursor = db.query(
                WeatherDatabaseHelper.TABLE_WEATHER,   // The table to query
                projection,                            // The array of columns to return (null to return all)
                selection,                             // The columns for the WHERE clause
                selectionArgs,                         // The values for the WHERE clause
                null,                                  // Don't group the rows
                null,                                  // Don't filter by row groups
                sortOrder                              // The sort order
        );

        // Process the cursor if it contains data
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Extract data from the cursor
                String cityName = cursor.getString(cursor.getColumnIndexOrThrow(WeatherDatabaseHelper.COLUMN_CITY_NAME));
                String temperature = cursor.getString(cursor.getColumnIndexOrThrow(WeatherDatabaseHelper.COLUMN_TEMPERATURE));
                String windSpeed = cursor.getString(cursor.getColumnIndexOrThrow(WeatherDatabaseHelper.COLUMN_WIND_SPEED));
                String condition = cursor.getString(cursor.getColumnIndexOrThrow(WeatherDatabaseHelper.COLUMN_CONDITION));

                // You can use this data to update your UI or perform any other operations

                // Example: Log the retrieved data
                Log.d(TAG, "City: " + cityName + ", Temperature: " + temperature + ", Wind Speed: " + windSpeed + ", Condition: " + condition);

                // Example: Display the data in a TextView
                cityNameTV.setText(cityName);
                temperatureTV.setText(temperature);

                // Format wind speed and condition for display in TextView
                String formattedWeather = "Wind Speed: " + windSpeed + " km/h\n\nCondition: " + condition;
                conditionTV.setText(formattedWeather);

                // Add more processing as needed

            } while (cursor.moveToNext()); // Move to the next row
        }

        // Close the cursor and the database
        if (cursor != null) {
            cursor.close();
        }
        db.close();
    }


    private void loadBookmarks() {
        bookmarksListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                bookmarksList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String bookmarkedCity = snapshot.getKey();
                    bookmarksList.add(bookmarkedCity);
                }
                updateBookmarkButton();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MainActivity", "Error loading bookmarks", databaseError.toException());
            }
        };

        bookmarksRef.addListenerForSingleValueEvent(bookmarksListener);
    }

    private void updateBookmarkButton() {
        String cityName = cityNameTV.getText().toString();
        if (bookmarksList.contains(cityName)) {
            bookmarkButton.setImageResource(R.drawable.bookmarked);
        } else {
            bookmarkButton.setImageResource(R.drawable.baseline_bookmark_border_24);
        }
    }

    public static void openDrawer(DrawerLayout drawerLayout) {
        drawerLayout.openDrawer(GravityCompat.START);

    }

    public static void closeDrawer(DrawerLayout drawerLayout) {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    public static void redirectActivity(Activity activity, Class secondActivity) {
        Intent intent = new Intent(activity, secondActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.finish();

    }

    protected void onPause() {
        super.onPause();
        closeDrawer(drawerLayout);

    }

    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update the UI accordingly.
        FirebaseUser currentUser = auth.getCurrentUser();
        updateUI(currentUser);
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            // User is signed in
            userEmailTextView.setText(user.getEmail());
        } else {
            // User is signed out
            userEmailTextView.setText("Not signed in");
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted..", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please provide the permissions", Toast.LENGTH_SHORT).show();
                //finish();
            }
        }
    }

    @Override
    public void onRequestPermissions(String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }

    // The rest of your existing methods (getCityName, getWeatherInfo, etc.)

    private String getCityName(double longitude, double latitude) {
        String cityName = "";  // Default to an empty string
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());

        try {
            List<Address> addresses = gcd.getFromLocation(latitude, longitude, 10);

            for (Address adr : addresses) {
                if (adr != null) {
                    String city = adr.getLocality();
                    if (city != null && !city.equals("")) {
                        cityName = city;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("CityName", "Resolved City Name: " + cityName);
        return cityName;
    }


    private void getWeatherInfo(String cityName) {
        Log.d("WeatherApp", "getWeatherInfo called for city: " + cityName);
        String url = "https://api.weatherapi.com/v1/forecast.json?key=242ceaef0654493098e55318231512&q=" + cityName + "&days=1&aqi=yes&alerts=yes";
        cityNameTV.setText(cityName);
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("WeatherApp", "API Response: " + response.toString());
                loadingPB.setVisibility(View.GONE);
                homeRL.setVisibility(View.VISIBLE);
                weatherRVModelArrayList.clear();

                try {
                    if (selectedTemperatureMetric.equals("Celsius")) {
                        // Display temperature in Celsius
                        String temperature = response.getJSONObject("current").getString("temp_c");
                        temperatureTV.setText(temperature + "°C");
                    } else if (selectedTemperatureMetric.equals("Fahrenheit")) {
                        // Convert temperature to Fahrenheit and display
                        String temperatureCelsius = response.getJSONObject("current").getString("temp_c");
                        double temperatureFahrenheit = Double.parseDouble(temperatureCelsius) * 9 / 5 + 32;
                        temperatureTV.setText(String.format(Locale.getDefault(), "%.2f", temperatureFahrenheit) + "°F");
                    }
                    int isDay = response.getJSONObject("current").getInt("is_day");
                    JSONObject currentWeather = response.getJSONObject("current");
                    String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                    String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                    String windSpeedKph = currentWeather.getString("wind_kph");
                    // Get the wind speed in mph
                    String windSpeedMph = currentWeather.getString("wind_mph");
                    String temperature = response.getJSONObject("current").getString("temp_c");

                    if (selectedWindSpeedMetric.equals("km/h")) {
                        // Display wind speed in km/h
                        conditionTV.setText(condition + "\n\n\nWind Speed: " + windSpeedKph + " km/h");
                    } else if (selectedWindSpeedMetric.equals("mph")) {
                        // Display wind speed in mph
                        conditionTV.setText(condition + "\n\n\nWind Speed: " + windSpeedMph + " mph");
                    }


                    // Set condition and wind speed text
                    Picasso.get().load("http:".concat(conditionIcon)).into(iconIV);

                    if (isDay == 1) {
                        //morning
                        Picasso.get().load("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSvmQu7BpcTQoc4z0RYl-yjonxC96DIR-d0HxZ5J0pbI6ZJOmPKF_ZP6SASw-kB_o4vpD8&usqp=CAU").into(backIV);
                    } else {
                        Picasso.get().load("https://w0.peakpx.com/wallpaper/725/98/HD-wallpaper-moon-clouds-cool-night-purple-weather.jpg").into(backIV);
                    }

                    JSONObject forecastObj = response.getJSONObject("forecast");
                    JSONObject forecastO = forecastObj.getJSONArray("forecastday").getJSONObject(0);
                    JSONArray hourArray = forecastO.getJSONArray("hour");
                    for (int i = 0; i < hourArray.length(); i++) {
                        JSONObject hourObj = hourArray.getJSONObject(i);
                        String time = hourObj.getString("time");
                        String temper = hourObj.getString("temp_c");
                        String img = hourObj.getJSONObject("condition").getString("icon");
                        String wind = hourObj.getString("wind_kph");
                        weatherRVModelArrayList.add(new WeatherRVModal(time, temper, img, wind));
                    }
                    weatherRVAdapter.notifyDataSetChanged();
                    insertDataIntoDatabase(cityName, temperature, windSpeedKph, condition);
                    notifyUser(cityName, condition, temperatureTV.getText().toString());


                } catch (JSONException e) {
                    Log.e("WeatherApp", "Error: " + e.toString());
                    Toast.makeText(MainActivity.this, "Error: " + e.toString(), Toast.LENGTH_SHORT).show();
                    queryDataFromDatabase();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Toast.makeText(MainActivity.this, "Please enter valid city name..", Toast.LENGTH_SHORT).show();
                Log.e("WeatherApp", "Error: " + error.toString());
                Toast.makeText(MainActivity.this, "Error: " + error.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        requestQueue.add(jsonObjectRequest);
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (auth.getCurrentUser() != null) {
            loadBookmarks();
        }
    }
    private void notifyUser(String cityName, String condition, String temperature) {
        // Check if the app has the necessary notification permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // Request the missing permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_CODE);
            return;
        }

        // Create the notification channel
        createNotificationChannel();

        // Build the notification content
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "weather_notification")
                .setSmallIcon(R.drawable.baseline_notifications_active_24)
                .setContentTitle("Weather Update")
                .setContentText("Current weather in " + cityName + ": " + condition + ", " + temperature)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Get the notification manager and send the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

}

