package com.example.bay.repository;

public interface IApiCallback<T> {

    void onSuccess(T result);

    void onError(String errorMessage);
}
