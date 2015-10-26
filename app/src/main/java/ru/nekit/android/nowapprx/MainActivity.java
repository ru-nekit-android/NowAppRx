package ru.nekit.android.nowapprx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        IntentFilter broadcastFilter = new IntentFilter();
        broadcastFilter.addAction(LoadEventsService.NOTIFICATION_LOAD_PAGE);
        broadcastFilter.addAction(LoadEventsService.NOTIFICATION_OBTAIN_EVENT);
        broadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case LoadEventsService.NOTIFICATION_LOAD_PAGE:

                        Log.v("ru,nekit.android.vtag", action + ": " + intent.getIntExtra(LoadEventsService.KEY_PAGE_NUMBER, 0));

                        break;

                    case LoadEventsService.NOTIFICATION_OBTAIN_EVENT:

                        Log.v("ru,nekit.android.vtag", action + ": " + intent.getStringExtra(LoadEventsService.KEY_EVENT));

                        break;

                    default:
                        break;
                }

            }
        }, broadcastFilter);
        startService(new Intent(this, LoadEventsService.class));
    }
}
