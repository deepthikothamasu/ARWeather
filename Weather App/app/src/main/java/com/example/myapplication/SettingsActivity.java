package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    ImageView menu;
    LinearLayout home, settings, share, about, logout;
    private FirebaseAuth auth;
    private RadioGroup radioGroupTemperatureMetric, radioGroupAppTheme;
    private Button buttonSaveSettings,otherapp;
    private RadioGroup radioGroupWindSpeed;
    private String selectedWindSpeedMetric;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        SharedPreferences sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE);
        String temperatureMetric = sharedPreferences.getString("temperatureMetric", "Celsius");
        String appTheme = sharedPreferences.getString("appTheme", "Light");

        radioGroupTemperatureMetric = findViewById(R.id.radioGroupTemperature);
        radioGroupAppTheme = findViewById(R.id.radioGroupAppTheme);
        buttonSaveSettings = findViewById(R.id.buttonSave);
        otherapp=findViewById(R.id.otherapp);
        radioGroupWindSpeed = findViewById(R.id.radioGroupWindSpeed);


        if (temperatureMetric.equals("Celsius")) {
            radioGroupTemperatureMetric.check(R.id.radioButtonCelsius);
        } else {
            radioGroupTemperatureMetric.check(R.id.radioButtonFahrenheit);
        }

        if (appTheme.equals("Light")) {
            radioGroupAppTheme.check(R.id.radioButtonLight);
        } else {
            radioGroupAppTheme.check(R.id.radioButtonDark);
        }
        selectedWindSpeedMetric = sharedPreferences.getString("windSpeedMetric", "km/h");

        if (selectedWindSpeedMetric.equals("km/h")) {
            radioGroupWindSpeed.check(R.id.radioButtonKmPerHour);
        } else {
            radioGroupWindSpeed.check(R.id.radioButtonMilesPerHour);
        }
        otherapp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchotherapp();
            }
        });



        buttonSaveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });

        drawerLayout = findViewById(R.id.drawerLayout);
        menu = findViewById(R.id.menu);
        home = findViewById(R.id.home);
        about = findViewById(R.id.about);
        logout = findViewById(R.id.logout);
        settings = findViewById(R.id.settings);
        share = findViewById(R.id.share);
        auth = FirebaseAuth.getInstance();

        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDrawer(drawerLayout);
            }
        });
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redirectActivity(SettingsActivity.this, MainActivity.class);
            }
        });
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recreate();
            }
        });
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redirectActivity(SettingsActivity.this, ShareActivity.class);
            }
        });
        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redirectActivity(SettingsActivity.this, AboutActivity.class);
            }
        });
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                auth.signOut();
                startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
    private void launchotherapp(){
        Intent intent = new Intent();
        intent.setClassName("com.Bilkul.weatherar", "com.unity3d.player.UnityPlayerActivity");
        PackageManager packageManager = getPackageManager();
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Weather AR app not installed", Toast.LENGTH_SHORT).show();
        }
    }


    private void saveSettings() {
        SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();

        int selectedTemperatureMetricId = radioGroupTemperatureMetric.getCheckedRadioButtonId();
        RadioButton selectedTemperatureMetricRadioButton = findViewById(selectedTemperatureMetricId);
        String selectedTemperatureMetric = selectedTemperatureMetricRadioButton.getText().toString();

        int selectedAppThemeId = radioGroupAppTheme.getCheckedRadioButtonId();
        RadioButton selectedAppThemeRadioButton = findViewById(selectedAppThemeId);
        String selectedAppTheme = selectedAppThemeRadioButton.getText().toString();
        int selectedWindSpeedId = radioGroupWindSpeed.getCheckedRadioButtonId();
        RadioButton selectedWindSpeedRadioButton = findViewById(selectedWindSpeedId);
        selectedWindSpeedMetric = selectedWindSpeedRadioButton.getText().toString();

        editor.putString("windSpeedMetric", selectedWindSpeedMetric);

        editor.putString("temperatureMetric", selectedTemperatureMetric);
        editor.putString("appTheme", selectedAppTheme);
        editor.apply();

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
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
}
