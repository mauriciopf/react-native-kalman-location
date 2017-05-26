package com.seunlanlege.kalmanlocation;

import com.seunlanlege.kalmanlocation.KalmanLocationManager;
import static com.seunlanlege.kalmanlocation.KalmanLocationManager.UseProvider;

import android.location.Location;
import android.location.LocationProvider;
import android.location.LocationListener;
import android.support.annotation.Nullable;
import android.util.Log;
import android.os.Bundle;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;


public class KalmanLocation extends ReactContextBaseJavaModule {
		public static final String REACT_CLASS = "RNKalmanLocation";

		private static final long GPS_TIME = 1000;

		private static final long NET_TIME = 5000;

		private static final long FILTER_TIME = 200;

		public static final String TAG = KalmanLocation.class.getSimpleName();

		ReactApplicationContext mReactContext;
		KalmanLocationManager mKalmanLocationManager;

    public KalmanLocation(ReactApplicationContext reactContext) {
        super(reactContext);

        mReactContext = reactContext;
    }


    @Override
    public String getName() {
        return REACT_CLASS;
    }


    @ReactMethod
    public void getLocation() {
						mKalmanLocationManager = new KalmanLocationManager(mReactContext.getApplicationContext());
						mKalmanLocationManager.requestLocationUpdates(
								UseProvider.GPS, FILTER_TIME, GPS_TIME, NET_TIME, mLocationListener, true);
    }

    @ReactMethod
				public void removeListener() {
						mKalmanLocationManager.removeUpdates(mLocationListener);
				}

		private LocationListener mLocationListener = new LocationListener() {

				@Override
				public void onLocationChanged(Location location) {

						try {
								double longitude;
								double latitude;
								String provider;
								float accuracy;

								longitude = location.getLongitude();
								latitude = location.getLatitude();
								provider = location.getProvider();
								accuracy = location.getAccuracy();

								WritableMap params = Arguments.createMap();
								params.putDouble("longitude", longitude);
								params.putDouble("latitude", latitude);
								params.putString("provider", provider);
								params.putDouble("accuracy", accuracy);

								sendEvent(mReactContext, "kalmanFilter", params);
						} catch (Exception e) {
								e.printStackTrace();
								Log.i(TAG, "Location services disconnected.");
						}
				}

				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {


				}

				@Override
				public void onProviderEnabled(String provider) {

				}

				@Override
				public void onProviderDisabled(String provider) {

				}
		};

    private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
        if (reactContext.hasActiveCatalystInstance()) {
            reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        } else {
            Log.i(TAG, "Waiting for CatalystInstance...");
        }
    }

}
