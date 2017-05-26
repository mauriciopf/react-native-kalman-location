react-native-kalman-location
============================

Acquires location via google play services and runs a kalman filter on the stream of updates.

### Installation
 
#### Install the package
```bash
yarn add react-native-kalman-location
```

Link Native Module

```bash
react-native link
```

or do it manually as described below:

### Manual Installation: Add it to your android project

* In `android/settings.gradle`

```gradle
...
include ':react-native-kalman-location', ':app'
project(':react-native-kalman-location').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-kalman-location/android/app')
```

* In `android/app/build.gradle`

```gradle
...
dependencies {
    ...
    compile project(':react-native-kalman-location')
}
```

* register module (in MainActivity.java)


**React Native**
```java
...
import com.seunlanlege.kalmanlocation.RNKalmanLocation;
...
public class MainActivity extends ReactActivity {
 ....
 @Override
 protected List<ReactPackage> getPackages() {
   return Arrays.<ReactPackage>asList(
     new MainReactPackage(),
     new RNKalmanLocation(),
   );
 }
}
```

#### Add Permissions and Google API to your Project

Add this to your AndroidManifest file;

``` xml
// file: android/app/src/main/AndroidManifest.xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```
Make sure this goes at the bottom of the `<application>` tag.
``` xml
	<uses-library android:name="com.google.android.maps" />
	<meta-data
        android:name="com.google.android.gms.version"
        android:value="@integer/google_play_services_version" />
```

## Example Usage
```javascript
import React, { PureComponent } from 'react';
import RNKalmanLocation from 'react-native-kalman-location';

import {
  Component,
  AppRegistry,
  DeviceEventEmitter,
  StyleSheet,
  Text,
  View,
} from 'react-native';

type State = {
	latitude: number,
	longitude: number,
	provider: 'gps' | 'kalman',
	accuracy: number,
}

export default class RNKalmanLocationExample extends PureComponent {

	state: State = {
		latitude: 0, 
		longitude: 0,
		provider: '',
		accuracy: 0,
	};

	componentDidMount() {
		// Register Listener Callback - has to be removed later
		this.evEmitter = DeviceEventEmitter.addListener('updateLocation', this.onLocationChange);
		// Initialize RNKalmanLocation to start emiting location
		RNKalmanLocation.getLocation();
	}

  onLocationChange = (e: State) => {
    this.setState(() => ({ ...e }));
  }

  componentWillUnmount() {
    // Stop listening for Events
    this.evEmitter.remove();
		//	Stop emiting location, this is required!!
		RNKalmanLocation.removeListener();
  }

  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.location}>
          {JSON.stringify(this.state, null, 4)}
        </Text>
      </View>
    );
  }
}

var styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  location: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  }
});

AppRegistry.registerComponent('RNKalmanLocationExample', () => RNKalmanLocationExample);
```
