package com.example.bay.viewmodel;

import android.net.Uri;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class CreateLocationViewModel extends ViewModel {

    public final MutableLiveData<ArrayList<Uri>> imageUris = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Uri> profileUri = new MutableLiveData<>(null);

    public final MutableLiveData<String> farmName = new MutableLiveData<>("");
    public final MutableLiveData<String> phone = new MutableLiveData<>("");
    public final MutableLiveData<String> about = new MutableLiveData<>("");
    public final MutableLiveData<String> selectedCategory = new MutableLiveData<>("Market");

    public final MutableLiveData<Double> latitude = new MutableLiveData<>(null);
    public final MutableLiveData<Double> longitude = new MutableLiveData<>(null);
    public final MutableLiveData<String> locationAddress = new MutableLiveData<>("");

    public final MutableLiveData<ArrayList<String>> growingList = new MutableLiveData<>(new ArrayList<>());

    public void setImages(List<Uri> list) {
        imageUris.setValue(new ArrayList<>(list));
    }

    public void addImage(Uri uri) {
        ArrayList<Uri> list = imageUris.getValue() == null ? new ArrayList<>() : imageUris.getValue();
        if (!list.contains(uri)) list.add(uri);
        imageUris.setValue(list);
    }

    public void removeImage(int index) {
        ArrayList<Uri> list = imageUris.getValue() == null ? new ArrayList<>() : imageUris.getValue();
        if (index >= 0 && index < list.size()) list.remove(index);
        imageUris.setValue(list);
    }

    public void addGrowing(String value) {
        ArrayList<String> list = growingList.getValue() == null ? new ArrayList<>() : growingList.getValue();
        list.add(value);
        growingList.setValue(list);
    }

    public void removeGrowing(int index) {
        ArrayList<String> list = growingList.getValue() == null ? new ArrayList<>() : growingList.getValue();
        if (index >= 0 && index < list.size()) list.remove(index);
        growingList.setValue(list);
    }
}
