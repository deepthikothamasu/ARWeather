package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

public class ShareActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    ImageView menu;
    LinearLayout home,settings,share,about,logout;
    private TextView tvTemperature;
    private WeatherAPIHandler weatherAPIHandler;
    private FirebaseAuth auth;
    private LineChart lineChart;
    private String cityName;
    private List<Double> temperatureValues = new ArrayList<>();
    private List<String> datesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        lineChart = findViewById(R.id.line_chart);

        // Fetch weather data and populate line chart
        drawerLayout=findViewById(R.id.drawerLayout);
        menu=findViewById(R.id.menu);
        home=findViewById(R.id.home);
        about=findViewById(R.id.about);
        logout=findViewById(R.id.logout);
        settings=findViewById(R.id.settings);
        share=findViewById(R.id.share);
        auth = FirebaseAuth.getInstance();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("cityName")) {
            cityName = intent.getStringExtra("cityName");
        }else {
            // Handle the case when city name is not provided
            Toast.makeText(this, "City name not provided", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if city name is not provided
        }

        TextView tvPlaceName = findViewById(R.id.tvPlaceName);
        tvPlaceName.setText(cityName);

        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDrawer(drawerLayout);
            }
        });
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redirectActivity(ShareActivity.this, MainActivity.class);
            }
        });
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redirectActivity(ShareActivity.this, SettingsActivity.class);

            }
        });
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recreate();

            }
        });
        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redirectActivity(ShareActivity.this, AboutActivity.class);

            }
        });
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                auth.signOut();

                // Redirect the user to the login screen or any other appropriate screen
                startActivity(new Intent(ShareActivity.this, LoginActivity.class));
                finish();

            }
        });
        tvTemperature = findViewById(R.id.tvTemperature);
        weatherAPIHandler = new WeatherAPIHandler(this);

        // Replace "London" with the desired city name
        weatherAPIHandler.getWeatherData(cityName, new WeatherAPIHandler.WeatherDataCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                // Parse the response and store temperature values
                parseWeatherData(cityName, response);
                // Plot the line chart using temperature values
                plotLineChart();
            }

            @Override
            public void onError(VolleyError error) {
                tvTemperature.setText("Error fetching data");
            }
        });
    }

    private void parseWeatherData(String cityName, JSONObject response) {
        try {
            JSONArray forecastList = response.getJSONArray("list");

            // Map to store daily temperature data, using TreeMap for sorting by date
            TreeMap<String, List<Double>> dailyTemperatureData = new TreeMap<>();

            for (int i = 0; i < forecastList.length(); i++) {
                JSONObject dayData = forecastList.getJSONObject(i);
                String date = dayData.getString("dt_txt").split(" ")[0]; // Extract date without time
                double temperatureKelvin = dayData.getJSONObject("main").getDouble("temp");

                // Convert temperature from Kelvin to Celsius
                double temperatureCelsius = temperatureKelvin - 273.15;

                // Add temperature to the list for the corresponding date
                if (dailyTemperatureData.containsKey(date)) {
                    dailyTemperatureData.get(date).add(temperatureCelsius);
                } else {
                    List<Double> temperatureList = new ArrayList<>();
                    temperatureList.add(temperatureCelsius);
                    dailyTemperatureData.put(date, temperatureList);
                }

            }
            datesList.clear();
            datesList.addAll(dailyTemperatureData.keySet());
            StringBuilder temperatureText = new StringBuilder("Average Temperature for the next 5 days:\n");

            for (Map.Entry<String, List<Double>> entry : dailyTemperatureData.entrySet()) {
                String date = entry.getKey();
                List<Double> temperatureList = entry.getValue();

                // Calculate average temperature for the day
                double averageTemperature = calculateAverageTemperature(temperatureList);


                temperatureText.append(String.format("%s: %.2f°C\n", date, averageTemperature));
            }

            tvTemperature.setText(temperatureText.toString());

            // Calculate average temperature for each day and store it in temperatureValues list
            temperatureValues.clear();
            for (Map.Entry<String, List<Double>> entry : dailyTemperatureData.entrySet()) {
                List<Double> temperatureList = entry.getValue();
                double averageTemperature = calculateAverageTemperature(temperatureList);
                temperatureValues.add(averageTemperature);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private double calculateAverageTemperature(List<Double> temperatureList) {
        double sum = 0;

        for (double temperature : temperatureList) {
            sum += temperature;
        }

        return sum / temperatureList.size();
    }

    private void plotLineChart() {
        List<Entry> entries = new ArrayList<>();

        // Loop through temperature values and add them to the line chart entries
        for (int i = 0; i < temperatureValues.size(); i++) {
            // Add the temperature value as a data point for the current date
            entries.add(new Entry(i, temperatureValues.get(i).floatValue()));
        }

        // Create a LineDataSet with the entries
        LineDataSet dataSet = new LineDataSet(entries, "Temperature");
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.BLACK);

        // Create a LineData object with the LineDataSet
        LineData lineData = new LineData(dataSet);

        // Set data to the LineChart
        lineChart.setData(lineData);

        // Disable grid lines
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getAxisLeft().setDrawGridLines(false);


        // Set x-axis labels to display dates
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < datesList.size()) {
                    // Return the date for the current data point
                    return datesList.get(index);
                }
                return "";
            }
        });

        // Set the number of labels to be displayed equal to the number of data points
        xAxis.setLabelCount(entries.size(), true);

        // Refresh the chart
        lineChart.invalidate();
    }

    public static void openDrawer(DrawerLayout drawerLayout){
        drawerLayout.openDrawer(GravityCompat.START);

    }
    public static void closeDrawer(DrawerLayout drawerLayout){
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }
    public static void redirectActivity(Activity activity, Class secondActivity){
        Intent intent=new Intent(activity,secondActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.finish();

    }
    protected void onPause(){
        super.onPause();
        closeDrawer(drawerLayout);

    }

}
