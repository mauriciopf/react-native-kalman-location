package com.seunlanlege.kalmanlocation;

import android.content.Context;
import android.location.LocationListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;


public class KalmanLocationManager {


    public enum UseProvider { GPS, NET, GPS_AND_NET }


    public static final String KALMAN_PROVIDER = "kalman";


    private static final String TAG = KalmanLocationManager.class.getSimpleName();


    private final Context mContext;


    private final Map<LocationListener, LooperThread> mListener2Thread;


    public KalmanLocationManager(Context context) {

        mContext = context;
        mListener2Thread = new HashMap<LocationListener, LooperThread>();
    }


    public void requestLocationUpdates(
            UseProvider useProvider,
            long minTimeFilter,
            long minTimeGpsProvider,
            long minTimeNetProvider,
            LocationListener listener,
            boolean forwardProviderReadings)
    {
        // Validate arguments
        if (useProvider == null)
            throw new IllegalArgumentException("useProvider can't be null");

        if (listener == null)
            throw new IllegalArgumentException("listener can't be null");

        if (minTimeFilter < 0) {

            Log.w(TAG, "minTimeFilter < 0. Setting to 0");
            minTimeFilter = 0;
        }

        if (minTimeGpsProvider < 0) {

            Log.w(TAG, "minTimeGpsProvider < 0. Setting to 0");
            minTimeGpsProvider = 0;
        }

        if (minTimeNetProvider < 0) {

            Log.w(TAG, "minTimeNetProvider < 0. Setting to 0");
            minTimeNetProvider = 0;
        }

        // Remove this listener if it is already in use
        if (mListener2Thread.containsKey(listener)) {

            Log.d(TAG, "Requested location updates with a listener that is already in use. Removing.");
            removeUpdates(listener);
        }

        LooperThread looperThread = new LooperThread(
                mContext, useProvider, minTimeFilter, minTimeGpsProvider, minTimeNetProvider,
                listener, forwardProviderReadings);

        mListener2Thread.put(listener, looperThread);
    }

    /**
     * Removes location estimates for the specified LocationListener.
     * <p>
     * Following this call, updates will no longer occur for this listener.
     *
     * @param listener Listener object that no longer needs location estimates.
     */
    public void removeUpdates(LocationListener listener) {

        LooperThread looperThread = mListener2Thread.remove(listener);

        if (looperThread == null) {

            Log.d(TAG, "Did not remove updates for given LocationListener. Wasn't registered in this instance.");
            return;
        }

        looperThread.close();
    }
}
