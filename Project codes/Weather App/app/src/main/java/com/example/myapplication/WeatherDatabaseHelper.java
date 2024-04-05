package com.example.myapplication;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class WeatherDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "weather.db";
    private static final int DATABASE_VERSION = 1;

    // Define the table name and column names
    public static final String TABLE_WEATHER = "weather";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_CITY_NAME = "city_name";
    public static final String COLUMN_TEMPERATURE = "temperature";
    public static final String COLUMN_WIND_SPEED = "wind_speed";
    public static final String COLUMN_CONDITION = "condition";
    // Add more columns as needed

    // SQL statement to create the weather table
    private static final String SQL_CREATE_TABLE = "CREATE TABLE " + TABLE_WEATHER + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_CITY_NAME + " TEXT NOT NULL, " +
            COLUMN_TEMPERATURE + " TEXT NOT NULL, " +
            COLUMN_WIND_SPEED + " TEXT NOT NULL, " +
            COLUMN_CONDITION + " TEXT NOT NULL)";

    public WeatherDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create the weather table
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed and create fresh table
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEATHER);
        onCreate(db);
    }
}
