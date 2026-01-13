package com.rnpushdy

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeArray
import com.facebook.react.module.annotations.ReactModule
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.reactNativePushdy.PushdySdk
import com.reactNativePushdy.RNPushdyData.convertDynamicFieldToJavaType
import com.reactNativePushdy.ReactNativeJson.convertMapToWritableMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit


@ReactModule(name = RnPushdyModule.NAME)
class RnPushdyModule(reactContext: ReactApplicationContext) :
  NativeRnPushdySpec(reactContext) {
  private val pushdySdk: PushdySdk =  PushdySdk(reactContext)

  // Queue for pending events when bridge is not ready
  private val pendingEvents = ConcurrentLinkedQueue<Pair<String, ReadableMap?>>()
  private val handler = Handler(Looper.getMainLooper())
  private var isProcessingQueue = false
  private var isReadyToSendEvent = false


  init {
    pushdySdk.setEventEmitter { eventName, params ->
      run {
        Log.d("Pushdy", "Event: $eventName | Params: $params")
        if (eventName == "onNotificationOpened") {
          emitOrQueueEvent("onNotificationOpened", params)
        } else if (eventName == "onNotificationReceived") {
          emitOrQueueEvent("onNotificationReceived", params)
        }
      }
    }
  }

  private fun emitOrQueueEvent(eventName: String, params: ReadableMap?) {
    try {
      // Check if ready to send events
      if (isReadyToSendEvent) {
        Log.d("Pushdy", "Ready to send, emitting: $eventName")
        if (eventName == "onNotificationOpened") {
          emitOnNotificationOpened(params)
        } else if (eventName == "onNotificationReceived") {
          emitOnNotificationReceived(params)
        }

        // Process any queued events after successfully emitting
        processQueuedEvents()
      } else {
        // Queue the event for later
        Log.w("Pushdy", "Bridge not ready, queueing: $eventName")
        pendingEvents.offer(Pair(eventName, params))
        scheduleQueueProcessing()
      }
    } catch (e: Exception) {
      // If emission fails, queue it
      Log.e("Pushdy", "Failed to emit $eventName, queueing: ${e.message}")
      pendingEvents.offer(Pair(eventName, params))
      scheduleQueueProcessing()
    }
  }

  private fun scheduleQueueProcessing() {
    if (!isProcessingQueue && pendingEvents.isNotEmpty()) {
      isProcessingQueue = true
      handler.postDelayed({
        processQueuedEvents()
      }, 500)
    }
  }

  private fun processQueuedEvents() {
    if (!isReadyToSendEvent) {
      Log.d("Pushdy", "Not ready to send events yet, will retry")
      isProcessingQueue = false
      if (pendingEvents.isNotEmpty()) {
        scheduleQueueProcessing()
      }
      return
    }

    Log.d("Pushdy", "Processing ${pendingEvents.size} queued events")
    isProcessingQueue = false

    while (pendingEvents.isNotEmpty()) {
      val (eventName, params) = pendingEvents.poll() ?: break
      try {
        Log.d("Pushdy", "Emitting queued event: $eventName")
        if (eventName == "onNotificationOpened") {
          emitOnNotificationOpened(params)
        } else if (eventName == "onNotificationReceived") {
          emitOnNotificationReceived(params)
        }
      } catch (e: Exception) {
        Log.e("Pushdy", "Failed to emit queued event $eventName: ${e.message}")
        // Re-queue if it fails
        pendingEvents.offer(Pair(eventName, params))
        break
      }
    }
  }
  override fun getName(): String {
    return NAME
  }

  // Example method
  // See https://reactnative.dev/docs/native-modules-android
  override fun multiply(a: Double, b: Double): Double {
    return a * b
  }

  override fun sampleMethod(stringArgument: String?, numberArgument: Double, promise: Promise?) {
    Log.d("Pushdy", "sampleMethod: $stringArgument | $numberArgument")
    try {
      TimeUnit.SECONDS.sleep(2)
    } catch (e: InterruptedException) {
      promise?.reject("SAMPLE_ERROR", e)
      e.printStackTrace()
      return
    }

    val array = WritableNativeArray()
    array.pushString("Received numberArgument: $numberArgument stringArgument: $stringArgument")
    array.pushInt((numberArgument * 2).toInt())

    promise?.resolve(array)
  }

  override fun makeCrash(promise: Promise?) {
    throw Exception("makeCrash from react-native-pushdy")
  }

  override fun initPushdy(options: ReadableMap?, promise: Promise?) {
    try {
      if (options != null) {
        pushdySdk.initPushdy(options)
      }
      promise?.resolve(true)
    } catch (e: Exception) {
      promise?.reject("INIT_ERROR", e)
    }
  }

  override fun isRemoteNotificationRegistered(promise: Promise?) {
    promise?.resolve(pushdySdk.isRemoteNotificationRegistered)
  }

  override fun isAppOpenedFromPush(promise: Promise?) {
    promise?.resolve(pushdySdk.isAppOpenedFromPush)
  }

  override fun isNotificationEnabled(promise: Promise?) {
    promise?.resolve(pushdySdk.isNotificationEnabled())
  }

  override fun registerForPushNotification(promise: Promise?) {
    // Only for iOS
    promise?.resolve(true);
  }

  override fun setApplicationIconBadgeNumber(count: Double, promise: Promise?) {
    // Only for iOS
    promise?.resolve(true);
  }

  override fun getApplicationIconBadgeNumber(promise: Promise?) {
    // Only for iOS
    promise?.resolve(true);
  }

  override fun setAttributeFromOption(options: ReadableMap?, promise: Promise?) {
    // Only for iOS
    promise?.resolve(true);
  }

  override fun startHandleIncommingNotification(promise: Promise?) {
    pushdySdk.startHandleIncomingNotification()
    promise?.resolve(true)
  }

  override fun stopHandleIncommingNotification(promise: Promise?) {
    pushdySdk.stopHandleIncomingNotification()
    promise?.resolve(true)
  }

  override fun getReadyForHandlingNotification(promise: Promise?) {
    pushdySdk.readyForHandlingNotification()
    promise?.resolve(true)
  }

  override fun setPushBannerAutoDismiss(autoDismiss: Boolean, promise: Promise?) {
    pushdySdk.setPushBannerAutoDismiss(autoDismiss);
    promise?.resolve(true);
  }

  override fun setPushBannerDismissDuration(sec: Double, promise: Promise?) {
    pushdySdk.setPushBannerDismissDuration(sec.toFloat());
    promise?.resolve(true);
  }

  override fun setCustomPushBanner(viewType: String?, promise: Promise?) {
    // ("Not yet implemented even in old version")
    promise?.resolve(true)
  }

  override fun useSDKHandler(enabled: Boolean, promise: Promise?) {
    pushdySdk.useSDKHandler(enabled);
    promise?.resolve(true);
  }

  override fun handleCustomInAppBannerPressed(notificationId: String?, promise: Promise?) {
    if (notificationId != null) {
      pushdySdk.handleCustomInAppBannerPressed(notificationId)
    };
    promise?.resolve(true);
  }

  override fun setCustomMediaKey(mediaKey: String?, promise: Promise?) {
    pushdySdk.setCustomMediaKey(mediaKey);
    promise?.resolve(true);
  }

  override fun setDeviceId(deviceId: String?, promise: Promise?) {
    pushdySdk.setDeviceId(deviceId);
    promise?.resolve(true);
  }

  override fun getDeviceId(promise: Promise?) {
    promise?.resolve(pushdySdk.getDeviceId());
  }

  override fun getDeviceToken(promise: Promise?) {
    promise?.resolve(pushdySdk.getDeviceToken());
  }

  override fun getPendingNotification(promise: Promise?) {
    promise?.resolve(pushdySdk.getPendingNotification());
  }

  override fun getPendingNotifications(promise: Promise?) {
    promise?.resolve(pushdySdk.getPendingNotifications());
  }

  override fun getInitialNotification(promise: Promise?) {
    // Mark as ready to send events and process queue
    isReadyToSendEvent = true
    Log.d("Pushdy", "getInitialNotification called, marking ready and processing queue")
    processQueuedEvents()
    promise?.resolve(pushdySdk.getInitialNotification());
  }

  override fun removeInitialNotification(promise: Promise?) {
    pushdySdk.removeInitialNotification();
    promise?.resolve(true);
  }

  override fun setAttributeFromValueContainer(
    attr: String?,
    valueContainer: ReadableMap?,
    commitImmediately: Boolean,
    promise: Promise?
  ) {
    val data = valueContainer!!.getDynamic("data")
    val value = convertDynamicFieldToJavaType(data)
    if (attr != null && value != null) {
      pushdySdk.setAttribute(attr, value, commitImmediately)
    }
    promise!!.resolve(true)
  }

  override fun pushAttributeArray(
    attr: String?,
    value: ReadableArray?,
    commitImmediately: Boolean,
    promise: Promise?
  ) {
    if (value != null && attr != null) {
      pushdySdk.pushAttribute(attr, value.toArrayList().toArray(), commitImmediately)
    };
    promise?.resolve(true)
  }

  override fun getPlayerID(promise: Promise?) {
    promise?.resolve(pushdySdk.getPlayerID());
  }

  override fun setBadgeOnForeground(enable: Boolean, promise: Promise?) {
    pushdySdk.setBadgeOnForeground(enable);
    promise?.resolve(true);
  }

  override fun setSubscribedEvents(subscribedEventNames: ReadableArray?, promise: Promise?) {
    val eventNames = ArrayList<String>()
    for (i in 0..<subscribedEventNames!!.size()) {
      if (subscribedEventNames.getType(i) === ReadableType.String) {
        val str = subscribedEventNames.getString(i)
        if (str != null) {
          eventNames.add(str)
        }
      }
    }

    pushdySdk.setSubscribedEvents(eventNames)

    // Trigger processing of queued events now that JS listeners are ready
    Log.d("Pushdy", "JS listeners registered, processing queue")
    processQueuedEvents()

    promise!!.resolve(true)
  }

  override fun getPendingEvents(count: Double, promise: Promise?) {
    val events = Arguments.createArray()
    val list: List<HashMap<String, Any>>? = pushdySdk.getPendingEvents(count)
    if (list != null) {
      for (i in list.indices) {
        val map = convertMapToWritableMap(list[i])
        events.pushMap(map)
      }
    }
    promise?.resolve(events)
  }

  override fun setPendingEvents(events: ReadableArray?, promise: Promise?) {
    val list: MutableList<HashMap<String?, Any?>> = ArrayList()
    val eventsList: List<*> = events!!.toArrayList()
    for (i in eventsList.indices) {
      val map = eventsList[i] as HashMap<String?, Any?>
      list.add(map)
    }
    pushdySdk.setPendingEvents(list)
  }

  override fun removePendingEvents(count: Double, promise: Promise?) {
    pushdySdk.removePendingEvents(count)
    promise?.resolve(true)
  }

  override fun pushPendingEvents(promise: Promise?) {
    pushdySdk.pushPendingEvents();
    promise?.resolve(true);
  }

  override fun setApplicationId(applicationId: String?, promise: Promise?) {
    if (applicationId != null) {
      pushdySdk.setApplicationId(applicationId)
    };
    promise?.resolve(true);
  }

  override fun trackEvent(
    eventName: String?,
    eventProperties: ReadableMap?,
    immediate: Boolean,
    promise: Promise?
  ) {
    if (eventName != null && eventProperties != null) {
      pushdySdk.trackEvent(eventName, eventProperties.toHashMap() as HashMap<String, Any>, immediate)
    };
    promise?.resolve(true);
  }



  override fun subscribe(promise: Promise?) {
    pushdySdk.subscribe();
    promise?.resolve(true);
  }

  override fun getAllBanners(promise: Promise?) {
    val list: JsonArray? = pushdySdk.getAllBanners()
    val banners = Arguments.createArray()
    if (list == null) {
      promise!!.resolve(banners)
      return
    }
    for (i in 0..<list.size()) {
      val map = Gson().fromJson(
        list[i],
        HashMap::class.java
      )

      val map1 = convertMapToWritableMap(map)
      banners.pushMap(map1)
    }
    promise!!.resolve(banners)
  }

  override fun trackBanner(bannerId: String?, type: String?, promise: Promise?) {
    if (bannerId != null && type != null) {
      pushdySdk.trackBanner(bannerId, type)
    };
    promise?.resolve(true);
  }

  override fun getBannerData(bannerId: String?, promise: Promise?) {
    val data: JsonObject? = bannerId?.let { pushdySdk.getBannerData(it) }

    // convert JsonObject to HashMap
    val map = Gson().fromJson(
      data,
      HashMap::class.java
    )

    val writableMap = convertMapToWritableMap(map)

    promise!!.resolve(writableMap)
  }

  companion object {
    const val NAME = "RnPushdy"
  }
}
