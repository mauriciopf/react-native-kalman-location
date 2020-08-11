package com.seunlanlege.kalmanlocation;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;


public class KalmanLocation extends ReactContextBaseJavaModule {
		public static final String REACT_CLASS = "RNKalmanLocation";

		private static final long FILTER_TIME = 200;

		public static final String TAG = KalmanLocation.class.getSimpleName();

		ReactApplicationContext mReactContext;
    	private LooperThread mLooper2Thread;


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
		mLooper2Thread = new LooperThread(mReactContext, FILTER_TIME);
    }

    @ReactMethod
	public void removeLocationUpdates() {
		mLooper2Thread.removeLocationUpdates();
				}
}
