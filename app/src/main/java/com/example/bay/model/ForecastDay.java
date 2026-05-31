package com.example.bay.model;

public class ForecastDay {

    public String dayLabel;
    public int minTemp;
    public int maxTemp;
    public int rainPercent;
    public String iconCode;
    public String description;

    // Constructor for forecast data with rain percentage
    public ForecastDay(String dayLabel, int minTemp, int maxTemp, int rainPercent, String iconCode) {
        this.dayLabel = dayLabel;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.rainPercent = rainPercent;
        this.iconCode = iconCode;
    }

    // Existing constructor (kept for compatibility, description may be unused)
    public ForecastDay(String dayLabel, int minTemp, int maxTemp, String description, String iconCode, int rainPercent) {
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

    public int getRainPercent() {
        return rainPercent;
    }

    public void setRainPercent(int rainPercent) {
        this.rainPercent = rainPercent;
    }

}
