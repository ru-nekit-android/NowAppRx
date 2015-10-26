package ru.nekit.android.nowapprx;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.squareup.okhttp.OkHttpClient;

import java.util.ArrayList;
import java.util.Iterator;

import retrofit.JacksonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;
import ru.nekit.android.nowapprx.network.ApiCallResult;
import ru.nekit.android.nowapprx.network.NowService;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class LoadEventsService extends Service {

    public static final String NOTIFICATION_LOAD_PAGE = "ru.nekit.android.nowapp.load_page";
    public static final String NOTIFICATION_OBTAIN_EVENT = "ru.nekit.android.nowapp.obtain_event";
    public static final String KEY_PAGE_NUMBER = "ru.nekit.android.nowapp.page_number";
    public static final String KEY_EVENT = "ru.nekit.android.nowapp.event";

    private static final int VERSION = 20;
    private String deviceToken;
    private int pageNumber;

    public LoadEventsService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        pageNumber = 1;
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        Func1<Long, Observable<JsonNode>> loadNextEvents = this::loadEvents;
        Observable.range(0, Integer.MAX_VALUE - 1)
                .scan(loadEvents(), (acc, index) -> acc.map(node -> {
                    Iterator<JsonNode> eventIterator = node.get("events").elements();
                    ArrayList<JsonNode> list = new ArrayList<>();
                    while (eventIterator.hasNext()) {
                        list.add(eventIterator.next());
                    }
                    Intent notification = new Intent();
                    notification.setAction(NOTIFICATION_LOAD_PAGE);
                    notification.putExtra(KEY_PAGE_NUMBER, pageNumber);
                    broadcastManager.sendBroadcast(notification);
                    pageNumber++;
                    return list;
                })
                        .flatMap(Observable::from)
                        .doOnNext(event ->{
                            Intent notification = new Intent();
                            notification.setAction(NOTIFICATION_OBTAIN_EVENT);
                            notification.putExtra(KEY_EVENT, event.toString());
                            broadcastManager.sendBroadcast(notification);
                        })
                        .last()
                        .map(event -> event.get("startAt").asLong())
                        .concatMap(loadNextEvents::call))
                .concatMap(o -> o)
                .takeUntil(node -> !node.get("hasNext").asBoolean())
                .subscribeOn(Schedulers.computation())
                .subscribe();

        return START_STICKY;
    }

    private Observable<ApiCallResult<JsonNode>> getApiCallResult(Func1<String, Observable<ApiCallResult<JsonNode>>> api) {
        Observable<String> register =
                getService()
                        .registerDevice(
                                1,
                                Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID),
                                "android",
                                VERSION
                        )
                        .flatMap(new ValidateServiceResponseOrThrow())
                        .map(value -> value.result.get("token").asText())
                        .doOnNext(value -> deviceToken = value)
                        .retry(3)
                        .cache();
        return Observable
                .concat(Observable.just(deviceToken), register)
                .first(value -> !TextUtils.isEmpty(value))
                .flatMap(api::call)
                .flatMap(new ValidateServiceResponseOrThrow())
                .retry(3);
    }

    private class ValidateServiceResponseOrThrow implements Func1<ApiCallResult<JsonNode>, Observable<ApiCallResult<JsonNode>>> {
        @Override
        public Observable<ApiCallResult<JsonNode>> call(ApiCallResult<JsonNode> response) {
            if (!"Success".equals(response.code) || response.httpCode != 200) {
                return Observable.error(new Exception());
            }
            return Observable.just(response);
        }
    }

    public Observable<JsonNode> loadEvents() {
        return loadEvents(0);
    }

    public Observable<JsonNode> loadEvents(long startAt) {
        NowService service = getService();
        int count = 12;
        return getApiCallResult(token -> startAt == 0 ? service.getEvents(token, count) : service.getNextEvents(token, startAt, count)).map(result -> result.result);
    }

    private NowService getService() {
        OkHttpClient client = new OkHttpClient();
        Retrofit retrofit = new Retrofit.Builder().baseUrl(NowService.BASE_URL)
                .client(client)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        return retrofit.create(NowService.class);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
