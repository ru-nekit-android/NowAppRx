package ru.nekit.android.nowapprx.network;

import com.fasterxml.jackson.databind.JsonNode;

import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;

public interface NowService {


    public static final String BASE_URL = "http://nowapp.ru";

    @GET("api/v2/device/register")
    Observable<ApiCallResult<JsonNode>> registerDevice(@Query("city_id") int cityId, @Query("device_id") String deviceId, @Query("platform") String platform, @Query("version") int version);

    @GET("api/v2/events")
    Observable<ApiCallResult<JsonNode>> getEvents(@Query("token") String token, @Query("n") int n);

    /*Unused
    @GET("api/v2/events")
    Observable<JsonNode> getEventsRaw(@Query("token") String token, @Query("n") int n);
    */

    @GET("api/v2/events")
    Observable<ApiCallResult<JsonNode>> getNextEvents(@Query("token") String token, @Query("start_at") long startAt, @Query("n") int n);


}
