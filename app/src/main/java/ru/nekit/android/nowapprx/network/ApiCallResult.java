package ru.nekit.android.nowapprx.network;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by MacOS on 29.09.15.
 */
public class ApiCallResult<T> {

    public String code;
    public int httpCode;
    public String message;

    @JsonProperty("data")
    public T result;
}

