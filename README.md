This application is a demo of the capabilities of using Google Nearby for sending and receiving data. 

## Getting Started with Google Nearby

* Add a new build rule in your application module's `build.gradle` file under dependencies for Google Play Services, e.g. `compile 'com.google.android.gms:play-services-nearby:11.4.2'`.

* Initialize a new `GoogleApiClient` object and connect it to Google Play Services

* Implement `GoogleApiClient.ConnectionCallbacks`
  * This interface contains callbacks that are called when the Google API Client has connected successfully or has suspended connection.
  * After the client has connected successfully, we can start advertising and discovering devices.
  
* Implement `GoogleApiClient.OnConnectionFailedListener`

* Create a class that extends `ConnectionLifecycleCallback`
  * The logic for connecting to endpoints goes here. `onConnectionInitiated`, `onConnectionResult`, and `onDisconnected` should be overridden. 
  * A connection can be accepted in `onConnectionInitiated`. `Nearby.Connections.acceptConnection` contains a callback that is called when a payload is received.
  
* Implement `Nearby.Connections.startAvertising` and `Nearby.Connections.startDiscovery`
  * Both methods have a service ID parameter. The application will only discover devices that are advertising the given service ID.

* Create a new `EndpointDiscoveryCallback`
  * This class contains callbacks for when endpoints are found or lost.

* Sending a byte payload simply requires calling `Nearby.Connections.sendPayload`.
  
