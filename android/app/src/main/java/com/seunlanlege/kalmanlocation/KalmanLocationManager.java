package com.seunlanlege.kalmanlocation;

import android.content.Context;
import android.location.LocationListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;


public class KalmanLocationManager {

    public static final String KALMAN_PROVIDER = "kalman";


    private static final String TAG = KalmanLocationManager.class.getSimpleName();


    private final Context mContext;


    private LooperThread mLooper2Thread;


    public KalmanLocationManager(Context context) {
        mContext = context;
    }


    public void requestLocationUpdates(long minTimeFilter) {
        if (minTimeFilter < 0) {
            Log.w(TAG, "minTimeFilter < 0. Setting to 0");
            minTimeFilter = 0;
        }

        mLooper2Thread = new LooperThread(mContext, minTimeFilter);
    }
}
