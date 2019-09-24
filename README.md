# Pushdy


## Example


## Requirements


## Installation
**Using Jitpack**
Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:
```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

Step 2. Add the dependency
```gradle
dependencies {
    implementation 'com.github.Pushdy:android-pushdy-sdk:0.0.1'
}
```
	
## Usage

**Import**

Import module in Kotlin language:
```Kotlin
import com.pushdy.Pushdy
```

Import module in Java language
```Java
import com.pushdy.Pushdy;
```

**Initialization**

In onCreate method of an application class, initialize Pushdy SDK as below:

```Kotlin
// Kotlin language
import com.pushdy.Pushdy
class ExampleApplication : Application(), Pushdy.PushdyDelegate {
    
    override fun onCreate() {
        super.onCreate()
        val clientKey = "your-client-key"
        Pushdy.initWith(this, clientKey, this)
    }
}

```

```Java
// Java language
import com.pushdy.Pushdy;
public class TestApplication extends Application implements Pushdy.PushdyDelegate {
    @Override
    public void onCreate() {
        super.onCreate();
        String clientKey = "your-client-key";
        Pushdy.initWith(this, clientKey, this);
    }
}
```

You can implement PushdyDelegate interface or not, it depends on your using purpose.

**Methods**

- getDeviceToken

Get device token from pushdy


```Kotlin
// Kotlin language
Pushdy.getDeviceToken()
```

```Java
// Java language
Pushdy.getDeviceToken();
```


- isNotificationEnabled

Check allowing notification or not


```Kotlin
// Kotlin language
Pushdy.isNotificationEnabled()
```

```Java
// Java language
Pushdy.isNotificationEnabled();
```


- setDeviceID

Using your device id instead of Pushdy device id

```Kotlin
// Kotlin language
val deviceID = "your device id"
Pushdy.setDeviceID(deviceID)
```

```Java
// Java language
String deviceID = "your device id";
Pushdy.setDeviceID(deviceID);
```


- getPendingNotification

Get pending notification which is not handled

```Kotlin
// Kotlin language
Pushdy.getPendingNotification()
```

```Java
// Java language
Pushdy.getPendingNotification();
```

- setAttribute

Set value for an attribute. You can set third param (commitImmediately variable)  to true to commit your value immediately.

```Kotlin
// Kotlin language
Pushdy.setAttribute("network_carrier", "your_network_carrier") 

// Equivalent to
Pushdy.setAttribute("network_carrier", "your_network_carrier", false)
```

```Java
// Kotlin language
Pushdy.setAttribute("network_carrier", "your_network_carrier");

// Equivalent to
Pushdy.setAttribute("network_carrier", "your_network_carrier", false);
```

- pushAttribute

Push value into a type of array attributes. You can set third param (commitImmediately variable)  to true to commit your value immediately.

```Kotlin
// Kotlin language
val value:String = ...
Pushdy.pushAttribute("name", value)

// Equivalent to
Pushdy.pushAttribute("name", value, false)
```

```Java
// Java language

String value = ...
Pushdy.pushAttribute("name", value);

// Equivalent to
Pushdy.pushAttribute("name", value, false);
```

**Pushdy Delegation**

For listen the Pushdy callback and adapt your logic with Pushdy, you must implement PushdyDelegate in your App Delegate

```Kotlin
// Kotlin language
import com.pushdy.Pushdy
class ExampleApplication : Application(), Pushdy.PushdyDelegate {
    
}
```

```Java
// Java language
import com.pushdy.Pushdy;
public class TestApplication extends Application implements Pushdy.PushdyDelegate {
   
}
```


-readyForHandlingNotification :

Determine that the application can handle push notification or not. Default is true. 
If false, incoming push will be pushed to pending notifications and you can process pending notifications later.

```Kotlin
// Kotlin language
override fun readyForHandlingNotification() : Boolean {
    var already = true
    // Example: already = pass through login or tutorial/introdution screen
    return already
}
```

```Java
// Java language
@Override
public boolean readyForHandlingNotification() {
    boolean already = true;
        // Example: already = pass through login or tutorial/introduction screen
    return already;
}
```


-onNotificationReceived:fromState :

When the application received a notification, Pushdy will trigger this method.

```Kotlin
// Kotlin language
override fun onNotificationReceived(notification: Map<String, Any>, fromState: String) {
        
}

```

```Java
// Java language
@Override
public void onNotificationReceived(@NotNull Map<String, ?> notification, @NotNull String fromState) {

}
```


-onNotificationOpened:fromState :

When user tap push notification banner (system notification or in app notification banner), Pushdy will trigger this method.

```Kotlin
// Kotlin language
override fun onNotificationOpened(notification: Map<String, Any>, fromState: String) {
         
}

```

```Java
// Java language
@Override
public void onNotificationOpened(@NotNull Map<String, ?> notification, @NotNull String fromState) {

}
```


And some other delegate methods...

**Customize In App Notification Banner**

We use PDYNotificationView view for default displaying in app push notification.
Pushdy also provides some method to adjust default notification view and set your custom view.

- setPushBannerAutoDismiss :

Turn on/off auto dismiss for in app notification banner.

```Kotlin
// Kotlin language
Pushdy.setPushBannerAutoDismiss(true)
```

```Java
// Java language
Pushdy.setPushBannerAutoDismiss(true);
```


- setPushBannerDismissDuration : 

Set auto dismiss duration for default custom view.

```Kotlin
// Kotlin language
Pushdy.setPushBannerDismissDuration(5) // 5 seconds
```

```Java
// Java language
Pushdy.setPushBannerDismissDuration(5); // 5 seconds
```


- setCustomPushBanner :

Set custom notification banner view. Implementating PDYPushBannerActionInterface interface is required.

```Kotlin
// Kotlin language
val yourCustomView = ...
Pushdy.setCustomPushBanner(yourCustomView)
```

```Java
// Java language
View yourCustomView = ...
Pushdy.setCustomPushBanner(yourCustomView);
```

*** Note: 

Pushdy SDK use media_url key for displaying thumbnail image from json push payload as default.
```ruby
{
   "aps" : {
        ...
   },
   "media_url" : "https://domain.com/path/image.png"
}
```

If you want to custom your own key, use setCustomMediaKey method for override it.
```Kotlin
// Kotlin language
PDYNotificationView.setCustomMediaKey("your_custom_media_key")
```

```Java
// Java language
PDYNotificationView.setCustomMediaKey("your_custom_media_key");
```


## Author

Pushdy Team, contact@pushdy.com

## License

Pushdy is available under the MIT license. See the LICENSE file for more info.
