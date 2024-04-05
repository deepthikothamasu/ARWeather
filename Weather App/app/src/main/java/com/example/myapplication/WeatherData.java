package com.example.myapplication;

public class WeatherData {
    private int id;
    private String city;
    private double temperature;
    private double windSpeed;
    private String condition;

    // Constructor
    public WeatherData(int id, String city, double temperature, double windSpeed, String condition) {
        this.id = id;
        this.city = city;
        this.temperature = temperature;
        this.windSpeed = windSpeed;
        this.condition = condition;
    }

    // Getters and setters
}