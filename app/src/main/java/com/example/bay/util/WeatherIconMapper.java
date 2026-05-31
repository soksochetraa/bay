package com.example.bay.util;

import com.example.bay.R;

/**
 * Utility class to map OpenWeatherMap icon codes to local drawable resources.
 * The method returns the appropriate drawable resource id for the given icon code.
 */
public class WeatherIconMapper {

    /**
     * Returns a drawable resource id based on the OpenWeatherMap icon code.
     *
     * @param iconCode the weather icon code from the API (e.g., "01d", "09n", "10d")
     * @return drawable resource id (e.g., R.drawable.sunny)
     */
    public static int getIconResource(String iconCode) {
        if (iconCode == null) {
            return R.drawable.pcloudy; // fallback
        }
        // The first two characters indicate the weather group.
        // See https://openweathermap.org/weather-conditions
        String prefix = iconCode.length() >= 2 ? iconCode.substring(0, 2) : "";
        switch (prefix) {
            case "01": // clear sky
                return R.drawable.pcloudy;
            case "02": // few clouds
            case "03": // scattered clouds
            case "04": // broken clouds
                return R.drawable.cloudy;
            case "09": // shower rain
            case "10": // rain
                return R.drawable.rainy;
            case "11": // thunderstorm
                return R.drawable.tstorm;
            case "13": // snow
                return R.drawable.snowy;
            case "50": // mist
                return R.drawable.mist; // assuming a mist drawable exists, otherwise fallback
            default:
                return R.drawable.pcloudy; // generic partially cloudy fallback
        }
    }
}
