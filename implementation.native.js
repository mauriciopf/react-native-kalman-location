import {RNKalmanLocation, GeolocationEventEmitter} from './nativeInterface';

let subscriptions = [];

const Geolocation = {
  watchPosition: function(success, error) {
    RNKalmanLocation.getLocation();
    const watchID = subscriptions.length;
    subscriptions.push([
      GeolocationEventEmitter.addListener("geolocationDidChange", success),
      error
        ? GeolocationEventEmitter.addListener("geolocationError", error)
        : null
    ]);
    return watchID;
  },

  clearWatch: function(watchID) {
    const sub = subscriptions[watchID];
    if (!sub) {
      // Silently exit when the watchID is invalid or already cleared
      // This is consistent with timers
      return;
    }

    sub[0].remove();
    // array element refinements not yet enabled in Flow
    const sub1 = sub[1];
    sub1 && sub1.remove();
    subscriptions[watchID] = undefined;
    let noWatchers = true;
    for (let ii = 0; ii < subscriptions.length; ii++) {
      if (subscriptions[ii]) {
        noWatchers = false; // still valid subscriptions
      }
    }
    if (noWatchers) {
      RNKalmanLocation.removeLocationUpdates();
      subscriptions = [];
    }
  },
};

module.exports = Geolocation;
