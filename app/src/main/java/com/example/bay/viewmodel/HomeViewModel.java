package com.example.bay.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<Double> temperatureLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> weatherIconLiveData = new MutableLiveData<>();

    public void setWeatherData(double temperature, String icon) {
        temperatureLiveData.postValue(temperature);
        weatherIconLiveData.postValue(icon);
    }

    public LiveData<Double> getTemperature() {
        return temperatureLiveData;
    }

    public LiveData<String> getWeatherIcon() {
        return weatherIconLiveData;
    }
}
