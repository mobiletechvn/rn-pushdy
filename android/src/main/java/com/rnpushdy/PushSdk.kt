package com.reactNativePushdy

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.common.LifecycleState
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.pushdy.Pushdy
import com.pushdy.Pushdy.PushdyDelegate
import com.rnpushdy.RnPushdyModule
import org.json.JSONException
import org.json.JSONObject

object  PushdySdkManager {
  fun registerActivityLifecycleCallbacks(context: Context, clientKey: String) {
    Log.d("RNPushdy", "registerActivityLifecycleCallbacks onActivityCreated")
    //TODO: Change initWith(context) to use better function name like initSdk or registerSdk
    // Because initWith(context) is misleading with other initWith(context, clientKey, ...)
    Pushdy.initWith(context)
    Pushdy.initWith(context, clientKey, null)
  }
}

class PushdySdk(reactContext: ReactApplicationContext) : PushdyDelegate, ActivityLifecycleCallbacks {

  private val mainAppContext: Context? = null
  private val smallIcon: Int? = null
  private var deviceId: String? = null
  private var readyForHandlingNotification = true
  private var reactContext: ReactApplicationContext = reactContext

  /**
   * This is list of event name those JS was already subscribed
   * Save this list to ensure that event will be sent to JS successfully
   *
   * - If this set size = 0 then JS event handler was not ready
   * - A default `enableFlag` event was subscribed to ensure that Set size always > 0 if JS was ready to handle
   * So If RNPushdyJS subscribe to no event, we will subscribe to default `enableFlag` event
   */
  private var subscribedEventNames: Set<String> = HashSet()

  /**
   * Return is app open from push or not
   * @return boolean
   */
  /**
   * This should return true when app open from push when in background or when app was killed
   */
  var isAppOpenedFromPush: Boolean = false


  /**
   * onRemoteNotificationRegistered fired when react context was not ready
   * OR when react-native-pushdy's JS thread have not subscribed to event
   * The result is your event will be fired and forgot,
   * Because of that, we change onRemoteNotificationRegistered to isRemoteNotificationRegistered
   */
  var isRemoteNotificationRegistered: Boolean = false

  private var eventEmitter: ((String, ReadableMap?) -> Unit)? = null

  // Set the event emitter callback from RnPushdyModule
  public fun setEventEmitter(eventEmitter: (String, ReadableMap?) -> Unit) {
    this.eventEmitter = eventEmitter
  }

  private fun sendEventTurbo(eventName: String, params: WritableMap?) {
    Log.d("RNPushdy", "sendEventTurbo: $eventName")
    eventEmitter?.invoke(eventName, params)
  }

  override fun onRemoteNotificationRegistered(deviceToken: String) {
    this.isRemoteNotificationRegistered = true

    val params = Arguments.createMap()
    params.putString("deviceToken", deviceToken)
    sendEventTurbo("onRemoteNotificationRegistered", params)

  }

  override fun onActivityPaused(activity: Activity) {
    Log.d("RNPushdy", "onActivityPaused")
    if (this.isAppOpenedFromPush) {
      this.isAppOpenedFromPush = false
    }
  }




  @Throws(Exception::class)
  fun initPushdy(options: ReadableMap) {
    var deviceId: String? = ""
    if (!options.hasKey("deviceId")) {
      throw Exception("RNPushdy.initPushdy: Invalid param: options.deviceId is required")
    } else {
      deviceId = options.getString("deviceId")

      if (deviceId == null || deviceId === "") {
        throw Exception("RNPushdy.initPushdy: Invalid param: options.deviceId cannot be empty")
      }
    }

    val clientKey: String = options.getString("clientKey") ?: ""
    if (clientKey == "") {
      throw Exception("RNPushdy.initPushdy: Invalid param: options.clientKey cannot be empty")
    }

    val applicationContext = reactContext.applicationContext as Application
    val smallIcon = reactContext.resources.getIdentifier("ic_notification", "mipmap", reactContext.packageName)

    // This need to call to make sure that PushdySDK trigger onSession.
    Pushdy.setNullDeviceID()
    Pushdy.initWith(applicationContext, clientKey, this, smallIcon)
    Pushdy.setDeviceID(deviceId)
    Pushdy.registerForRemoteNotification()
    Pushdy.setBadgeOnForeground(true)
    applicationContext.registerActivityLifecycleCallbacks(this)

    this.deviceId = deviceId
  }

  fun isNotificationEnabled(): Boolean {
    return Pushdy.isNotificationEnabled()
  }

  fun startHandleIncomingNotification() {
    readyForHandlingNotification = true
  }

  fun stopHandleIncomingNotification() {
    readyForHandlingNotification = false
  }

  override fun readyForHandlingNotification(): Boolean {
    return readyForHandlingNotification
  }

  fun setPushBannerAutoDismiss(autoDismiss: Boolean) {
    Pushdy.setPushBannerAutoDismiss(autoDismiss)
  }

  fun setPushBannerDismissDuration(sec: Float) {
    Pushdy.setPushBannerDismissDuration(sec)
  }

  fun useSDKHandler(enabled: Boolean) {
    Pushdy.useSDKHandler(enabled)
  }

  fun handleCustomInAppBannerPressed(notificationId: String) {
    Pushdy.handleCustomInAppBannerPressed(notificationId)
  }

  fun setCustomMediaKey(mediaKey: String?) {
    // Pushdy.setCustomMediaKey(mediaKey);
  }

  fun setDeviceId(deviceId: String?) {
    if (deviceId != null) {
      this.deviceId = deviceId
      Pushdy.setDeviceID(deviceId)
    }
  }

  fun getDeviceId(): String? {
    return Pushdy.getDeviceID()
  }

  fun getDeviceToken(): String? {
    return Pushdy.getDeviceToken()
  }

  fun getPendingNotification(): WritableMap? {
    val notification = Pushdy.getPendingNotification() ?: return null

    var data: WritableMap = WritableNativeMap()

    try {
      val jo = JSONObject(notification)
      data = ReactNativeJson.convertJsonToMap(jo)
    } catch (e: JSONException) {
      e.printStackTrace()
      Log.e("RNPushdy", "getPendingNotification Exception " + e.message)
    }

    return data
  }

  fun getPendingNotifications(): List<WritableMap> {
    val notifications = Pushdy.getPendingNotifications()
    val items: MutableList<WritableMap> = ArrayList()
    for (notification in notifications) {
      var data: WritableMap = WritableNativeMap()
      try {
        val jo = JSONObject(notification)
        data = ReactNativeJson.convertJsonToMap(jo)
      } catch (e: JSONException) {
        e.printStackTrace()
        Log.e("RNPushdy", "getPendingNotification Exception " + e.message)
      }

      items.add(data)
    }

    return items
  }

  fun getInitialNotification(): WritableMap? {
    val notificationStr: String = Pushdy.getInitialNotification()  ?: return null
    val notification = convertNotificationToMap(notificationStr, "closed")
    return notification
  }

  fun removeInitialNotification() {
    Pushdy.removeInitialNotification()
  }

  fun setInitialNotification(notification: String) {
    Pushdy.setInitialNotification(notification)
  }

  fun setAttribute(attr: String, value: Any, commitImmediately: Boolean) {
    Pushdy.setAttribute(attr, value, commitImmediately)
  }

  fun pushAttribute(attr: String, value: Array<Any?>, commitImmediately: Boolean) {
    // NOTE: Pushdy.pushAttribute used for pushing every array element, it does not support to push array of elements
    // This might be bug of android SDK, so please do not use this function until SDK confirmed
    Pushdy.pushAttribute(attr, value, commitImmediately)
  }

  fun getPlayerID(): String? {
    return Pushdy.getPlayerID()
  }

  fun setBadgeOnForeground(enable: Boolean) {
    Pushdy.setBadgeOnForeground(enable)
  }

  fun setSubscribedEvents(subscribedEventNames: ArrayList<String>) {
    this.subscribedEventNames = HashSet(subscribedEventNames)
  }

  fun getPendingEvents(count: Number?): List<HashMap<String, Any>>? {
    return Pushdy.getPendingEvents((count as Int?)!!)
  }

  fun setPendingEvents(events: List<HashMap<String?, Any?>?>) {
    val convertedEvents = events.filterNotNull().map { it.filterKeys { key -> key != null } as HashMap<String, Any> }
    Pushdy.setPendingEvents(convertedEvents.toMutableList())
  }

  fun setApplicationId(applicationId: String) {
    Pushdy.setApplicationId(applicationId)
  }

  fun removePendingEvents(count: Number?) {
    Pushdy.removePendingEvents((count as Int?)!!)
  }

  fun trackEvent(eventName: String, attributes: HashMap<String, Any>, immediate: Boolean) {
    Pushdy.trackEvent(eventName, attributes, immediate, null, null)
  }

  fun pushPendingEvents() {
    Pushdy.pushPendingEvents(null, null)
  }

  fun subscribe() {
    Pushdy.subscribe()
  }

  fun getAllBanners(): com.google.gson.JsonArray? {
    return Pushdy.getAllBanners()
  }

  fun trackBanner(bannerId: String, type: String) {
    Pushdy.trackBanner(bannerId, type)
  }

  fun getBannerData(bannerId: String): com.google.gson.JsonObject? {
    return Pushdy.getBannerData(bannerId)
  }

  override fun customNotification(
    title: String,
    body: String,
    image: String,
    data: Map<String, Any>
  ): Notification? {
    TODO("Not yet implemented")
  }

  private fun convertNotificationToMap(notification: String, fromState: String): WritableMap {
    var noti: WritableMap = WritableNativeMap()
    try {
      val jo = JSONObject(notification)
      noti = ReactNativeJson.convertJsonToMap(jo)
    } catch (e: JSONException) {
      e.printStackTrace()
      Log.e("RNPushdy", "onNotificationReceived Exception " + e.message)
    }

    val params = Arguments.createMap()
    params.putString("fromState", fromState)
    params.putMap("notification", RNPushdyData.toRNPushdyStructure(noti))
    params.putString("raw", notification)

    return params
  }

  override fun onNotificationOpened(notification: String, fromState: String) {
    Log.d("RNPushdy", "onNotificationOpened: notification: $notification")
    val params = convertNotificationToMap(notification, fromState)

    sendEventTurbo("onNotificationOpened", params)
    this.isAppOpenedFromPush = true
  }

  override fun onNotificationReceived(notification: String, fromState: String) {
    Log.d("RNPushdy", "onNotificationReceived: notification: $notification")

    var noti: WritableMap = WritableNativeMap()
    try {
      val jo = JSONObject(notification)
      noti = ReactNativeJson.convertJsonToMap(jo)
    } catch (e: JSONException) {
      e.printStackTrace()
      Log.e("RNPushdy", "onNotificationReceived Exception " + e.message)
    }

    val params = Arguments.createMap()
    params.putString("fromState", fromState)
    params.putMap("notification", RNPushdyData.toRNPushdyStructure(noti))

    sendEventTurbo("onNotificationReceived", params)
  }

  override fun onRemoteNotificationFailedToRegister(e: Exception) {
    val params = Arguments.createMap()
    params.putString("exceptionMessage", e.message)
    sendEventTurbo("onRemoteNotificationFailedToRegister", params)
  }

  //TODO: New Implement beyond here

  /**
   * Send event from native to JsThread
   * If the reactConetext has not available yet, => retry
   */
  private fun sendEvent(eventName: String, params: WritableMap? = null, retryCount: Int = 0) {
    var delayRetry = 1000L
    var maxRetry = 5

    /**
     * When you wake up your app from BG/closed state,
     * JS thread might not be available or ready to receive event
     */
    val jsThreadState = reactContext.lifecycleState
    val jsHandlerReady = subscribedEventNames.size > 0
    val jsSubscribeThisEvent = subscribedEventNames.contains(eventName)

    // Log.d("RNPushdy", "this.subscribedEventNames.size() = " + this.subscribedEventNames.size());
//       Log.d("RNPushdy", "jsThreadState = " + jsThreadState);
//       Log.d("RNPushdy", "reactActivated = " + Boolean.toString(reactActivated));
    Log.d("RNPushdy", "jsHandlerReady event=" + eventName +" size="+ subscribedEventNames.size.toString())
    Log.d("RNPushdy", "subscribedEventNames = " + subscribedEventNames.toString())
    Log.d("RNPushdy", "jsThreadState = " + jsThreadState.toString())
    if (jsThreadState == LifecycleState.RESUMED) {
      if (jsHandlerReady) {
        Log.d("RNPushdy", "sendEvent: reactContext is ready to send event: $eventName")
        // Delay for some second to ensure react context work
        if (jsSubscribeThisEvent) {
          Log.d("RNPushdy", "sendEvent: reactContext is ready to send subscribe event: $eventName")
          // this.sendEventWithDelay(eventName, params, 0);
          reactContext
            .getJSModule(
              DeviceEventManagerModule.RCTDeviceEventEmitter::class.java
            )
            .emit(eventName, params)
          Log.d("RNPushdy", "sendEvent: Emitted: $eventName")
        } else {
          Log.d(
            "RNPushdy",
            "sendEvent: Skip because JS not register the $eventName"
          )
        }

        // exit function to Prevent retry
        return
      } else {
        // Continue to Retry section
        // JS handle was ready so we increase the retry interval
        delayRetry = 300L
        maxRetry = 100 // around 30 secs


      }
    } else {
      // if (!reactActivated) {
      //   // Reset if subscribedEventNames JS is not ready
      //   this.subscribedEventNames = new HashSet<>();
      // }
      // continue to retry section
      this.sendEventWithDelay(eventName, params, delayRetry)
    }

    // ====== If cannot send then retry: ====
    Log.e(
      "RNPushdy",
      "sendEvent: $eventName was skipped because reactContext is null or not ready"
    )
  }

  private fun sendEventWithDelay(eventName: String, params: WritableMap?, delay: Long) {
    Log.d("RNPushdy", "sendEventWithDelay: $eventName")
    Log.d("RNPushdy", "sendEventWithDelay: delay = $delay")
    // Create a new thread to delay
    Thread {
      try {
        Thread.sleep(delay)
        Log.d("RNPushdy", "sendEventWithDelay: sleep $delay ms")
        // Check if reactContext is ready
        if (reactContext.hasActiveCatalystInstance()) {
          Log.d("RNPushdy", "sendEventWithDelay: reactContext is ready to send event: $eventName")
          reactContext
            .getJSModule(
              DeviceEventManagerModule.RCTDeviceEventEmitter::class.java
            )
            .emit(eventName, params)
          Log.d("RNPushdy", "sendEventWithDelay: Emitted: $eventName")
        } else {
          Log.e(
            "RNPushdy",
            "sendEventWithDelay: reactContext is not ready to send event: $eventName"
          )
        }
      } catch (e: InterruptedException) {
        e.printStackTrace()
      }
    }.start()
  }

  /**
   * ===================  Pushdy hook =============================
   */
  /*========================
  Tried to support initPushdy SDK from JS whenever we want, without success.
  I stored the draft version here, to remind anyone who wanna init the SDK from JS,
  It's not possible at the moment, depend on PushdySDK and its working flow.
  // the OLD flow is:
  // 1. Init PushdySDK to do some required work (see the SDK)
  // 2. Whenever you wanna start Pushdy (often on JS App mounting), call setDeviceId, Pushdy SDK will create a `Player` on the dashboard
  // 3. From now on, PushdySDK is ready to work.
  Currently, the flow is:
  1. registerSdk to init native SDK and prepare
  2. Whenever you wanna start Pushdy (often on JS App mounting), call initPushdy, Pushdy SDK will create a `Player` on the dashboard
  3. From now on, PushdySDK is ready to work.

  public void old_registerSdk(android.content.Context mainAppContext, Integer smallIcon) {
    this.mainAppContext = mainAppContext;
    this.smallIcon = smallIcon;

    // Listen to activity change
    Pushdy.registerActivityLifecycle(mainAppContext);
  }

  public void old_initPushdy(ReadableMap options) throws Exception {
    String clientKey = "";
    if (!options.hasKey("clientKey")) {
      throw new Exception("RNPushdy.initPushdy: Invalid param: options.clientKey is required");
    } else {
      clientKey = options.getString("clientKey");

      if (clientKey == null) {
        throw new Exception("RNPushdy.initPushdy: Invalid param: options.clientKey cannot be empty");
      }
    }

    if (options.hasKey("deviceId")) {
      this.setDeviceId(options.getString("deviceId"));
    }

    if (this.smallIcon != null) {
      Pushdy.initWith(this.mainAppContext, clientKey, this, this.smallIcon);
    } else {
      Pushdy.initWith(this.mainAppContext, clientKey, this);
    }
    Pushdy.registerForRemoteNotification();
    Pushdy.setBadgeOnForeground(true);
  }
  ======================== */




  override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
  }

  override fun onActivityStarted(activity: Activity) {
    Log.d("RNPushdy", "onActivityStarted")
  }

  override fun onActivityResumed(activity: Activity) {
  }



  override fun onActivityStopped(activity: Activity) {
  }

  override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
  }

  override fun onActivityDestroyed(activity: Activity) {
  }



}
