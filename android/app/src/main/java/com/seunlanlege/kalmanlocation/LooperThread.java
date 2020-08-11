package com.seunlanlege.kalmanlocation;

import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

class LooperThread extends Thread {

    private static final int DEFAULT_ACCURACY = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
    private static final long DEFAULT_INTERVAL = 1000;
    private static final long DEFAULT_FASTEST_INTERVAL = 1000;
    private static final float DEFAULT_DISTANCE_FILTER = 100;
    public static final String KALMAN_PROVIDER = "kalman";

    private static final int THREAD_PRIORITY = 5;

    private static final double DEG_TO_METER = 111225.0;
    private static final double METER_TO_DEG = 1.0 / DEG_TO_METER;

    private static final double TIME_STEP = 1.0;
    private static final double COORDINATE_NOISE = 4.0 * METER_TO_DEG;
    private static final double ALTITUDE_NOISE = 10.0;

    private final ReactApplicationContext mContext;
    private final Handler mClientHandler;

    private final long mMinTimeFilter;

    // Thread
    private Looper mLooper;
    private Handler mOwnHandler;
    private Location mLastLocation;
    private boolean mPredicted;

    private Tracker1D mLatitudeTracker, mLongitudeTracker, mAltitudeTracker;

    // FuseLocationProvider
    private FusedLocationProviderClient mFusedProviderClient;
    private LocationRequest mLocationRequest;
    private int mLocationPriority = DEFAULT_ACCURACY;
    private long mUpdateInterval = DEFAULT_INTERVAL;
    private long mFastestInterval = DEFAULT_FASTEST_INTERVAL;
    private float mDistanceFilter = DEFAULT_DISTANCE_FILTER;

    /**
     *
     * @param context
     * @param minTimeFilter
     */
    LooperThread(ReactApplicationContext context, long minTimeFilter)
    {
        mContext = context;
        mFusedProviderClient = LocationServices.getFusedLocationProviderClient(mContext);
        createLocationRequest();
        mClientHandler = new Handler();
        mMinTimeFilter = minTimeFilter;
        start();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(mLocationPriority)
                .setInterval(mUpdateInterval)
                .setFastestInterval(mFastestInterval)
                .setSmallestDisplacement(mDistanceFilter);
    }

    @Override
    public void run() {

        setPriority(THREAD_PRIORITY);

        Looper.prepare();
        mLooper = Looper.myLooper();
        mFusedProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, mLooper);
        Looper.loop();
    }

    public void removeLocationUpdates() {
        if (mFusedProviderClient != null && mLocationCallback != null) {
            mFusedProviderClient.removeLocationUpdates(mLocationCallback);
            mLocationCallback = null;
        }
        mLooper.quit();
    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationAvailability(LocationAvailability locationAvailability) {
            if (!locationAvailability.isLocationAvailable()) {
                mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("geolocationError", buildError("Unable to retrieve location"));
            }
        }

        @Override
        public void onLocationResult(LocationResult locationResult) {
            final Location location = locationResult.getLastLocation();
            // Reusable
            final double accuracy = location.getAccuracy();
            double position, noise;

            // Latitude
            position = location.getLatitude();
            noise = accuracy * METER_TO_DEG;

            if (mLatitudeTracker == null) {

                mLatitudeTracker = new Tracker1D(TIME_STEP, COORDINATE_NOISE);
                mLatitudeTracker.setState(position, 0.0, noise);
            }

            if (!mPredicted)
                mLatitudeTracker.predict(0.0);

            mLatitudeTracker.update(position, noise);

            // Longitude
            position = location.getLongitude();
            noise = accuracy * Math.cos(Math.toRadians(location.getLatitude())) * METER_TO_DEG ;

            if (mLongitudeTracker == null) {

                mLongitudeTracker = new Tracker1D(TIME_STEP, COORDINATE_NOISE);
                mLongitudeTracker.setState(position, 0.0, noise);
            }

            if (!mPredicted)
                mLongitudeTracker.predict(0.0);

            mLongitudeTracker.update(position, noise);

            // Altitude
            if (location.hasAltitude()) {

                position = location.getAltitude();
                noise = accuracy;

                if (mAltitudeTracker == null) {

                    mAltitudeTracker = new Tracker1D(TIME_STEP, ALTITUDE_NOISE);
                    mAltitudeTracker.setState(position, 0.0, noise);
                }

                if (!mPredicted)
                    mAltitudeTracker.predict(0.0);

                mAltitudeTracker.update(position, noise);
            }

            // Reset predicted flag
            mPredicted = false;

            // Update last location
            if (location.getProvider().equals(LocationManager.GPS_PROVIDER)
                    || mLastLocation == null || mLastLocation.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {

                mLastLocation = new Location(location);
            }

            mClientHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                .emit("geolocationDidChange", locationToMap(new Location((location))));
                    }
                });

            // Enable filter timer if this is our first measurement
            if (mOwnHandler == null) {
                mOwnHandler = new Handler(mLooper, mOwnHandlerCallback);
                mOwnHandler.sendEmptyMessageDelayed(0, mMinTimeFilter);
            }
        }
    };

    private Handler.Callback mOwnHandlerCallback = new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {

            // Prepare location
            final Location location = new Location(KALMAN_PROVIDER);

            // Latitude
            mLatitudeTracker.predict(0.0);
            location.setLatitude(mLatitudeTracker.getPosition());

            // Longitude
            mLongitudeTracker.predict(0.0);
            location.setLongitude(mLongitudeTracker.getPosition());

            // Altitude
            if (mLastLocation.hasAltitude()) {

                mAltitudeTracker.predict(0.0);
                location.setAltitude(mAltitudeTracker.getPosition());
            }

            // Speed
            if (mLastLocation.hasSpeed())
                location.setSpeed(mLastLocation.getSpeed());

            // Bearing
            if (mLastLocation.hasBearing())
                location.setBearing(mLastLocation.getBearing());

            // Accuracy (always has)
            location.setAccuracy((float) (mLatitudeTracker.getAccuracy() * DEG_TO_METER));

            // Set times
            location.setTime(System.currentTimeMillis());

            if (Build.VERSION.SDK_INT >= 17)
                location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

            // Post the update in the client (UI) thread
            mClientHandler.post(new Runnable() {
                @Override
                public void run() {
                    mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                            .emit("geolocationDidChange", locationToMap(location));
                }
            });

            // Enqueue next prediction
            mOwnHandler.removeMessages(0);
            mOwnHandler.sendEmptyMessageDelayed(0, mMinTimeFilter);
            mPredicted = true;

            return true;
        }
    };

    private static WritableMap locationToMap(Location location) {
        WritableMap map = Arguments.createMap();
        WritableMap coords = Arguments.createMap();
        coords.putDouble("latitude", location.getLatitude());
        coords.putDouble("longitude", location.getLongitude());
        coords.putDouble("altitude", location.getAltitude());
        coords.putDouble("accuracy", location.getAccuracy());
        coords.putDouble("heading", location.getBearing());
        coords.putDouble("speed", location.getSpeed());
        map.putMap("coords", coords);
        map.putDouble("timestamp", location.getTime());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            map.putBoolean("mocked", location.isFromMockProvider());
        }

        return map;
    }

    public static WritableMap buildError(String message) {
        WritableMap error = Arguments.createMap();

        if (message != null) {
            error.putString("message", message);
        }

        return error;
    }
}
