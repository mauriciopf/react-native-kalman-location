import {NativeEventEmitter, NativeModules} from 'react-native';

const {RNKalmanLocation} = NativeModules;

// Produce an error if we don't have the native module
if (!RNKalmanLocation) {
  throw new Error(`RNKalmanLocation native module not available`);
}

/**
 * We export the native interface in this way to give easy shared access to it between the
 * JavaScript code and the tests
 */
let nativeEventEmitter = null;
module.exports = {
  RNKalmanLocation,
  get GeolocationEventEmitter() {
    if (!nativeEventEmitter) {
      nativeEventEmitter = new NativeEventEmitter(RNKalmanLocation);
    }
    return nativeEventEmitter;
  },
};
