package com.example.bay.model;

public class ForecastDay {

    public String dayLabel;
    public int minTemp;
    public int maxTemp;
    public String description;
    public String iconCode;

    public ForecastDay() {
    }

    public ForecastDay(String dayLabel, int minTemp, int maxTemp, String description, String iconCode) {
        this.dayLabel = dayLabel;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.description = description;
        this.iconCode = iconCode;
    }

    public String getDayLabel() {
        return dayLabel;
    }

    public void setDayLabel(String dayLabel) {
        this.dayLabel = dayLabel;
    }

    public int getMinTemp() {
        return minTemp;
    }

    public void setMinTemp(int minTemp) {
        this.minTemp = minTemp;
    }

    public int getMaxTemp() {
        return maxTemp;
    }

    public void setMaxTemp(int maxTemp) {
        this.maxTemp = maxTemp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconCode() {
        return iconCode;
    }

    public void setIconCode(String iconCode) {
        this.iconCode = iconCode;
    }
}
