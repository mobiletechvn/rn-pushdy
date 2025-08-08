import React, { forwardRef } from 'react';
import type { Ref } from 'react';
import { NativeEventEmitter, Platform } from 'react-native';
import type { EmitterSubscription, NativeModule } from 'react-native';
import MessageQueue from 'react-native/Libraries/BatchedBridge/MessageQueue';
import EventBus, { EventName } from './EventBus';
import { PushdyBanner } from './PushdyBanner';
import RnPushdy from './NativeRnPushdy';

// Make RnPushdy compatible with NativeEventEmitter
const RnPushdyModule = RnPushdy as unknown as NativeModule;

// Type definitions for PushdyBannerRef which should be exported from PushdyBanner.tsx
interface PushdyBannerRef {
  showBanner: () => void;
  hideBanner: () => void;
  capture: () => void;
}

// Type definitions
interface BannerListeners {
  onShow: (bannerId: string, ...args: any[]) => void;
  onHide: (bannerId: string, ...args: any[]) => void;
  onAction: (
    bannerId: string,
    action_type: string,
    extra_data: any,
    ...args: any[]
  ) => void;
  onError: (
    bannerId: string,
    error: string,
    action_type: string,
    ...args: any[]
  ) => void;
  tmp: ReturnType<typeof setTimeout> | null;
}

interface PushdyBannerProps {
  onShow?: (bannerId: string, ...args: any[]) => void;
  onHide?: (bannerId: string, ...args: any[]) => void;
  onAction?: (
    bannerId: string,
    action_type: string,
    extra_data: any,
    ...args: any[]
  ) => void;
  onError?: (
    bannerId: string,
    error: string,
    action_type: string,
    ...args: any[]
  ) => void;
}

interface PushdyNotificationData {
  _notification_id?: string;
  _nms_image?: string;
  aps?: {
    alert?: {
      title?: string;
      body?: string;
    };
  };
  data?: {
    _notification_id?: string;
    _nms_image?: string;
    [key: string]: any;
  };
  [key: string]: any;
}

interface BannerData {
  id: string;
  track_id?: string;
  html: string;
  schedule_options?: {
    display_mode_max?: string | number;
    delay_time?: string | number;
    [key: string]: any;
  };
  [key: string]: any;
}

interface BannerTrackingData {
  imp?: number;
  [key: string]: any;
}

// Declare global window type for dev mode
declare global {
  interface Window {
    tmp_Pushdy?: RNPushdyWrapper;
  }
}

const forceDevEnv = false; // null mean env will not be force, false is force prod, true is force dev
const dev = __DEV__;
if (forceDevEnv != null ? forceDevEnv : dev) {
  MessageQueue.spy((msg) => {
    if (
      msg.module === 'RNPushdy' ||
      (msg.module === null && msg.method.toString().indexOf('RNPushdy') >= 0)
    ) {
      const fromTo = msg.type === 0 ? '[To JS]' : '[To Native]';
      const color = msg.type === 0 ? '#693' : '#639';
      console.log('%c' + fromTo + ' msg:', 'color: ' + color, msg);
    } else if (msg.module === 'RCTDeviceEventEmitter') {
      return;

      // // Ignore websocketMessage
      // if (msg.args && msg.args[0] === 'websocketMessage') {
      //   return;
      // }

      // const fromTo = msg.type === 0 ? '[To JS]' : '[To Native]';
      // const color = msg.type === 0 ? '#693' : '#639';
      // console.log(
      //   '%c' + fromTo + ' args, msg:',
      //   'color: ' + color,
      //   msg.args,
      //   msg
      // );
    }
  });
  console.log('{PushdyMessaging} Spy enabled: ');
} else {
  console.log('{PushdyMessaging} Spy disabled: ');
}

const isIos = Platform.OS === 'ios';
const isAndroid = Platform.OS === 'android';
const logStyle = {
  warning: 'background: orange',
  error: 'background: red',
};

const Test_Banner_HTML = `<div id="pd-c1631782-f955-4413-922e-c4600b008297"><div class="banner-pushdy wrap-card" style="background-image: url(https://cdn.24hmoney.vn/upload/img/thiep/tetam/2024/bg/newyear2024-3.jpg)">
              <div class="customers">
          <div class="greeting">Kính tặng</div>

          <div class="avatar">
            <img
              id="avatar_"
              src="https://cdn.24hmoney.vn/upload/img/thiep/avatar.png"
              alt=""
            />
          </div>
          <div class="userename"></div>
          <div></div>
        </div></div></div><style>@import url("https://fonts.cdnfonts.com/css/alexandria-4");
.wrap-card {
  display: flex;
  flex-direction: column;
  height: 600px;
  width: 337px;
  border-radius: 6px;
  background-repeat: no-repeat;
  background-size: 337px 600px;
}
.customers {
  position: relative;
  top: calc(74% - 3px);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-direction: column;
}
  #avatar_ {
    width: 74px;
    height: 74px;
    object-fit: cover;
    border-radius: 50%;
    margin-top: 8px;
    padding: 3px;
    background: #d58f14;
  }
.greeting {
  font-size: 12px;
  font-family: "Alexandria";
  color: #e1d8d8;
}
.userename {
  font-size: 16px;
  font-family: "alexandria";
  color: #e8e9db;
  margin-top: 8px;
}<style>`;

/**
 * Wrapper to Pushdy native module
 *
 * [x] Implement TimeToLive feature
 * [x] Use Promise style instead of traditional callback ==> Did it on native side
 * [x] Keep same api interface between iOS & Android => Handle it on native side
 * [x] User can catch promise, do not cast all result to resolve
 */

class RNPushdyWrapper {
  // Promise will reject if execution time is over this value in milisecs
  ttl = 10000;
  subscribers: { [key: string]: EmitterSubscription } = {};

  /**
   * @deprecated: Use _CustomInAppBannerComponent != null instead
   * @private
   */
  // _useSDKInAppBannerHandler = true
  _CustomInAppBannerComponent: React.Component | null = null;

  /**
   * @param {Number} ttl Time to live in miliseconds. Default to 10,000 ms
   */
  setTimeout(ttl: number) {
    this.ttl = ttl;
  }

  async sampleMethod(str: string, num: number) {
    return RnPushdy.sampleMethod(str, num);
  }

  async initPushdy(options: any) {
    return RnPushdy.initPushdy(options);
  }

  /**
   * Android only:
   * On android:
   *    registerForPushNotification was called automatically after PushdySDK's initilization
   *    true mean registered, false mean registering or failed.
   * On iOS:
   *    you need to call ios_registerForPushNotification manually from JS, that mean JS context was already be ready,
   *    so that you can listen to onRemoteNotificationRegistered event perfectly
   *    Or you can use isRemoteNotificationRegistered variable, it's depend!
   */
  async isRemoteNotificationRegistered() {
    return RnPushdy.isRemoteNotificationRegistered();
  }

  /**
   * https://guide.pushdy.com/i/tham-chieu-sdk-api/ios-native-sdk#registerforpushnotification
   *
   * @returns {Promise<void>}
   */
  async ios_registerForPushNotification() {
    if (isIos) {
      return RnPushdy.registerForPushNotification();
    } else {
      console.log(
        '%c{RnPushdy.ios_registerForPushNotification} support iOS only: ',
        logStyle.warning
      );
      return false;
    }
  }

  async isNotificationEnabled() {
    return RnPushdy.isNotificationEnabled();
  }

  /**
   * Turn on or off Pushdy built-in InAppBanner
   *
   * When you receive a notification in foreground:
   * - If enable: Pushdy SDK will show a notification in a built-in InAppBanner UI
   * - If NOT enable: Default to OS behavior
   *
   * Default to `true` on both android and ios
   *
   * @deprecated Please use `setCustomInAppBannerComponent` instead
   */
  async enablePushdyInAppBanner(enable: boolean) {
    if (isAndroid) {
      return RnPushdy.setBadgeOnForeground(enable);
    } else {
      console.error(
        '[WIP] TODO: Check if this function is supported on iOS. For now, You should use this function on Android only'
      );
      return false;
    }
  }

  /**
   *
   * @param {boolean} autoDismiss
   * @returns
   */
  async setPushBannerAutoDismiss(autoDismiss: boolean) {
    return RnPushdy.setPushBannerAutoDismiss(autoDismiss);
  }

  /**
   *
   * @param {number} sec
   * @returns
   */
  async setPushBannerDismissDuration(sec: number) {
    return RnPushdy.setPushBannerDismissDuration(sec);
  }

  /**
   *
   * @param {string} viewType
   * @returns
   */
  async setCustomPushBanner(viewType: string) {
    return RnPushdy.setCustomPushBanner(viewType);
  }

  /**
   * @param {string} mediaKey
   * @deprecated
   */
  async setCustomMediaKey(_mediaKey: string) {
    console.error('Do not supported');
    return false;
  }

  /**
   * You need to call this fn first
   * @param {string} id
   */
  async setDeviceId(id: string) {
    if (!id) {
      throw Error('setDeviceId: id cannot be empty');
    }

    return RnPushdy.setDeviceId(id);
  }

  async getDeviceId() {
    return RnPushdy.getDeviceId();
  }

  async getDeviceToken() {
    return RnPushdy.getDeviceToken();
  }

  async setReadyForHandlingNotification(enable: boolean) {
    return enable
      ? this.startHandleIncommingNotification()
      : this.stopHandleIncommingNotification();
  }

  async getReadyForHandlingNotification() {
    return RnPushdy.getReadyForHandlingNotification();
  }

  async startHandleIncommingNotification() {
    return RnPushdy.startHandleIncommingNotification();
  }

  async stopHandleIncommingNotification() {
    return RnPushdy.stopHandleIncommingNotification();
  }

  async getPendingNotification() {
    const a = await RnPushdy.getPendingNotification();
    return a ? new PushdyNotification(a) : undefined;
  }

  async getPendingNotifications() {
    const items = await RnPushdy.getPendingNotifications();
    if (Array.isArray(items)) {
      return items.map((i: any) => new PushdyNotification(i));
    }
    return [];
  }

  /**
   * Flow here:
   * When clicking notification from background or foreground. onNotificationOpened is triggered then
   * notification is saved.
   * getInitialNotification is used to re-trigger open notification when app restarts.
   * If you handled initicalNotification successful, please call removeInitalNotification.
   * Resolved issue: https://github.com/Pushdy/react-native-pushdy/issues/3
   * @return JSONObject
   */
  async getInitialNotification() {
    let a = await RnPushdy.getInitialNotification();
    return a ? new PushdyNotification(a) : null;
  }

  async removeInitialNotification() {
    return RnPushdy.removeInitialNotification();
  }

  /**
   * This method will return isAppOpenedFromPush = true if app opened from push (when app was killed).
   *
   * When app enters background (when opened from push) isAppOpenedFromPush will reset it's value (isAppOpenedFromPush = false).
   *
   * When app in background, then open push behavior will be different between each Platform (This method works in Android, currently not available in iOS)
   */
  async isAppOpenedFromPush() {
    return RnPushdy.isAppOpenedFromPush();
  }

  /**
   *
   * @param {string} attr
   * @param {any} value
   * @param {boolean} immediately
   * @returns
   */
  async setAttribute(attr: string, value: any, immediately = false) {
    if (value === null || value === undefined) {
      console.warn(
        '[Pushdy] ERROR: Invalid value argument, must not null/undefined instead of: ',
        value
      );
      return false;
    }

    // TODO: Reimplement setAttributeFromValueContainer for ios
    return isAndroid
      ? RnPushdy.setAttributeFromValueContainer(
          attr,
          { data: value },
          immediately
        )
      : RnPushdy.setAttributeFromOption({
          attr,
          data: value,
          immediately,
        });
  }

  /**
   * @param {String} attr
   * @param {Number|String|Array} value
   * @param {Boolean} value Persist data to Pushdy immediately or let Pushdy persist it by SDK schedule
   * @returns {Promise<Boolean>}
   */
  async pushAttribute(
    attr: string,
    value: number | string | any[],
    immediately = false
  ) {
    if (!Array.isArray(value)) {
      value = [value];
    }

    return RnPushdy.pushAttributeArray(attr, value, immediately);
  }

  async getPlayerID() {
    return RnPushdy.getPlayerID();
  }

  async makeCrash() {
    return RnPushdy.makeCrash();
  }

  /**
   *
   * @param {number} count
   * @returns
   */
  async setApplicationIconBadgeNumber(count: number) {
    if (isIos) {
      return RnPushdy.setApplicationIconBadgeNumber(count);
    } else {
      return undefined;
    }
  }

  async getApplicationIconBadgeNumber() {
    if (isIos) {
      return RnPushdy.getApplicationIconBadgeNumber();
    } else {
      return undefined;
    }
  }

  /**
   * Get pending events that haven't been sent to server yet from Pushdy SDK
   * @param {Number} count
   */
  async getPendingEvents(count = 50) {
    return RnPushdy.getPendingEvents(count);
  }

  /**
   * Set pending events that will be sent to server later
   * @param {{
   *  events: Record<string, any>[]
   * }[]} count
   */
  setPendingEvents(events: Record<string, any>[]) {
    return RnPushdy.setPendingEvents(events);
  }

  /**
   * Remove pending events that haven't been sent to server yet from Pushdy SDK.
   *  @param {number} count
   */
  async removePendingEvents(count: number) {
    return RnPushdy.removePendingEvents(count);
  }

  /**
   * set application id to Pushdy SDK for tracking purpose
   * @param {string} applicationId
   * @returns
   */
  setApplicationId(applicationId: string) {
    return RnPushdy.setApplicationId(applicationId);
  }

  /**
   * Track event to Pushdy SDK. This event will be sent to server later or immediately
   * base on `immediate` argument
   * @param {string} event
   * @param {Record<string, any>} params
   * @param {boolean} immediate
   */
  async trackEvent(
    event: string,
    params: Record<string, any>,
    immediate = false
  ) {
    return RnPushdy.trackEvent(event, params, immediate);
  }

  /**
   * Push pending events to server immediately.
   * @param {(response) => void} successCallback
   * @param {(code, message) => void} failureCallback
   */
  pushPendingEvents() {
    return RnPushdy.pushPendingEvents();
  }

  /**
   *
   * @param {*} notificationId
   * @returns
   */
  handleCustomInAppBannerPressed(notificationId: string) {
    // notice SDK that this notification was opened
    // console.log('{RNPushdyWrapper.handleCustomInAppBannerPressed} notificationId: ', notificationId);
    return RnPushdy.handleCustomInAppBannerPressed(notificationId);
  }

  /**
   * Show in app banner on foreground by using `Pushdy built-in InAppBanner` or `using custom JS banner`
   *
   * When you receive a notification in foreground:
   * - If component is null: Pushdy SDK will show a notification in a built-in InAppBanner UI, by native view, defined inside SDK
   * - If component is instance: Pushdy SDK will show a notification in a custom view
   *
   * Usage:
   * Please see the:
   * If component is instance => You take 100% control how the UI look / visible via component state,
   *
   * @param {CustomInAppBannerBaseView} component React component instance (not class definition), should inherit from  `react-native-pushdy/CustomInAppBannerBaseView`
   *                                              Because CustomInAppBannerBaseView already do some SDK communication, you don't need to care about logic, care your UI only
   *
   * @returns {Promise<boolean>}
   */
  setCustomInAppBannerComponent(component: React.Component | null) {
    this._CustomInAppBannerComponent = component;
    return RnPushdy.useSDKHandler(component === null);
  }

  removeCustomInAppBannerComponent() {
    return this.setCustomInAppBannerComponent(null);
  }

  getCustomInAppBannerComponent() {
    return this._CustomInAppBannerComponent;
  }

  bannerListeners: BannerListeners = {
    onShow: () => {},
    onHide: () => {},
    onAction: () => {},
    onError: () => {},
    tmp: null,
  };

  onShowPushdyBanner = (bannerId: string, ...args: any[]) => {
    this.bannerListeners.onShow(bannerId, ...args);
    this.trackBanner(bannerId, 'impression');
  };

  onHidePushdyBanner = (bannerId: string, ...args: any[]) => {
    this.bannerListeners.onHide(bannerId, ...args);
    this.trackBanner(bannerId, 'close');
  };

  onActionPushdyBanner = (
    bannerId: string,
    action_type: string,
    extra_data: any,
    ...args: any[]
  ) => {
    this.bannerListeners.onAction(bannerId, action_type, extra_data, ...args);
    this.trackBanner(bannerId, 'click');
  };

  onErrorPushdyBanner = (
    bannerId: string,
    error: string,
    action_type: string,
    ...args: any[]
  ) => {
    this.bannerListeners.onError(bannerId, error, action_type, ...args);
  };

  /**
   * To show a banner on foreground.
   *
   * @param {{
   *    onShow: (bannerId: string) => void,
   *    onHide: (bannerId: string) => void,
   *    onAction: (bannerId: string, action_type: "save" | "share", extra_data: any) => void,
   *    onError: (bannerId: string, error: string, action_type: string) => void,
   * }} props
   */
  initialPushdyBanner = (props: PushdyBannerProps) => {
    this.bannerListeners.onHide = props.onHide || (() => {});
    this.bannerListeners.onShow = props.onShow || (() => {});
    this.bannerListeners.onAction = props.onAction || (() => {});
    this.bannerListeners.onError = props.onError || (() => {});
    // register for event of PushdyBanner.
    EventBus.on(EventName.ON_SHOW_PUSHDY_BANNER, this.onShowPushdyBanner);
    EventBus.on(EventName.ON_HIDE_PUSHDY_BANNER, this.onHidePushdyBanner);
    EventBus.on(EventName.ON_ACTION_PUSHDY_BANNER, this.onActionPushdyBanner);
    EventBus.on(EventName.ON_ERROR_PUSHDY_BANNER, this.onErrorPushdyBanner);
    // send a event that ensure it's ready to show banner if needed
    EventBus.emit(EventName.READY_TO_SHOW_PUSHDY_BANNER);

    this.checkAndShowPushdyBannerIfHave();
  };

  hidePushdyBanner = () => {
    EventBus.emit(EventName.HIDE_PUSHDY_BANNER);
  };

  mapIdWithTrackId: { [key: string]: string } = {};

  checkAndShowPushdyBannerIfHave = async () => {
    const banners = (await this.getAllBanners()) as BannerData[];
    console.log(
      '{RNPushdyWrapper.checkAndShowPushdyBanner} banners: ',
      banners
    );

    if (Array.isArray(banners)) {
      for (let i = 0; i < banners.length; i++) {
        const banner = banners[i];
        if (!banner) continue;

        // map trackId with bannerId
        this.mapIdWithTrackId[banner.id] = banner.track_id || banner.id;

        const trackingBannerData = (await this.getBannerTrackingData(
          banner.id
        )) as BannerTrackingData;
        console.log(
          '{RNPushdyWrapper.checkAndShowPushdyBanner} -> trackingBannerData:',
          trackingBannerData
        );

        const options = banner?.schedule_options || {};
        // if banner is already shown, then skip it
        if (
          trackingBannerData &&
          typeof trackingBannerData.imp === 'number' &&
          trackingBannerData.imp >= Number(options.display_mode_max)
        ) {
          continue;
        } else {
          this.trackBanner(banner.id, 'loaded');

          this.bannerListeners.tmp = setTimeout(
            () => {
              EventBus.emit(EventName.SHOW_PUSHDY_BANNER, {
                html: banner.html,
                bannerId: banner.id,
                bannerData: banner,
              });
            },
            Number(options.delay_time) || 0
          );
          break;
        }
      }
    }
  };

  disposePushdyBanner = () => {
    // unregister for event of PushdyBanner.
    EventBus.off(EventName.ON_SHOW_PUSHDY_BANNER, this.onShowPushdyBanner);
    EventBus.off(EventName.ON_HIDE_PUSHDY_BANNER, this.onHidePushdyBanner);
    EventBus.off(EventName.ON_ACTION_PUSHDY_BANNER, this.onActionPushdyBanner);
    EventBus.off(EventName.ON_ERROR_PUSHDY_BANNER, this.onErrorPushdyBanner);

    // clear timeout
    clearTimeout(this.bannerListeners.tmp as ReturnType<typeof setTimeout>);
  };

  subscribe = () => {
    return RnPushdy.subscribe();
  };

  getAllBanners = async () => {
    return RnPushdy.getAllBanners();
  };

  /**
   * @param {string} bannerId
   * @param {'impression' | 'loaded' | 'close' | 'click'} type
   */
  trackBanner = async (
    bannerId: string,
    type: 'impression' | 'loaded' | 'close' | 'click'
  ) => {
    // call trackBanner with track_id
    if (
      this.mapIdWithTrackId[bannerId] &&
      this.mapIdWithTrackId[bannerId] != bannerId
    ) {
      const trackId = this.mapIdWithTrackId[bannerId];
      RnPushdy.trackBanner(trackId, type);
    }
    return RnPushdy.trackBanner(bannerId, type);
  };

  /**
   * @param {string} bannerId
   */
  getBannerTrackingData = (bannerId: string) => {
    return RnPushdy.getBannerData(bannerId);
  };

  __testPushdyBanner = () => {
    __DEV__ &&
      console.log(
        '{RNPushdyWrapper.__testPushdyBanner} Test_Banner_HTML: ',
        Test_Banner_HTML
      );
    EventBus.emit(EventName.SHOW_PUSHDY_BANNER, {
      html: Test_Banner_HTML,
      bannerId: 'test_banner_id',
    });
  };

  /**
   *
   * @param {{
   *  bottomView?: React.Component,
   *  topView?: React.Component,
   *  userName?: string,
   *  userAvatar?: string,
   *  userGender?: 'male' | 'female',
   * }} props
   *
   * Example: You can use some method of PushdyBanner to control banner
   * - ref.current.showBanner() => show banner
   * - ref.current.hideBanner() => hide banner
   * - ref.current.capture() => capture banner
   * @returns
   */
  PushdyBanner = forwardRef((props: any, ref: Ref<PushdyBannerRef>) => {
    return <PushdyBanner {...props} ref={ref} />;
  });

  /**
   * ========= Hooks ============
   */

  /**
   * To see a list of supported events and its data structure
   * See guide on https://guide.pushdy.com/
   *
   * @param {{}} listeners {[eventName]: function onEventNameTriggered() => {}}
   */
  startSubscribers(
    listeners: { [eventName: string]: (event: any) => void } = {}
  ) {
    const eventEmitter = new NativeEventEmitter(RnPushdyModule);

    const keys = Object.keys(listeners);
    for (let i = 0, c = keys.length; i < c; i++) {
      const eventName = keys[i];
      if (eventName) {
        const listener = listeners[eventName];

        if (
          eventName === 'onNotificationReceived' ||
          eventName === 'onNotificationOpened'
        ) {
          // Convert notification to PushdyNotification
          this.subscribers[eventName] = eventEmitter.addListener(
            eventName,
            (event) => {
              event.notification = new PushdyNotification(event.notification);
              if (listener) listener(event);
            }
          );
        } else {
          this.subscribers[eventName] = eventEmitter.addListener(
            eventName,
            (_event) => {
              if (listener) listener(_event);
            }
          );
        }
      }
    }

    /**
     * On some android devices, you need to check if events was successfully subscribed then you can send events to JS
     * Otherwise, Native will send event while NativeEventEmitter.addListener was not ready => Cause event was lost
     *
     * Reproduction:
     *  1. Press a noti in noti center while app is in BG / closed state
     *  2. App opened > React was init > JS was init / re-int(if BG) > subscribed again > Ready to receive event
     *  3. Native send a onNotificationOpen event when JS is not "Ready to receive event"
     */
    if (isAndroid) {
      // Read more about "enableFlag" at com.reactNativePushdy.PushdySdk#subscribedEventNames
      this.subscribers.enableFlag = eventEmitter.addListener(
        'enableFlag',
        () => {
          /* empty callback to avoid unused variable warning */
        }
      );
      RnPushdy.setSubscribedEvents(keys);
    }
  }

  onNotificationOpened = (
    handler: (notification: PushdyNotification) => void
  ) => {
    RnPushdy.onNotificationOpened((notificationRaw) => {
      const notification = new PushdyNotification(notificationRaw);
      handler(notification);
    });
  };

  onNotificationReceived = (
    handler: (notification: PushdyNotification) => void
  ) => {
    RnPushdy.onNotificationReceived((notificationRaw) => {
      const notification = new PushdyNotification(notificationRaw);
      handler(notification);
    });
  };

  stopSubscribers() {
    const keys = Object.keys(this.subscribers);
    for (let i = 0, c = keys.length; i < c; i++) {
      const k = keys[i];
      if (k && this.subscribers[k]) {
        this.subscribers[k].remove();
      }
    }
    this.subscribers = {};
  }
}

export class PushdyNotification {
  id: string | null = null;
  title: string | null = null;
  subtitle: string | null = null;
  body: string | null = null;
  image: string | null = null;

  // The custom data
  data: Record<string, any> = {};

  android: Record<string, any> = {}; //
  ios: Record<string, any> = {}; // aps: {"alert":{"title":"***","body":"***"},"mutable-content":1,"sound":{"volume":10,"name":"default","critical":1}}

  _KeyAlias: Record<string, string> = {
    _notification_id: 'id',
    _nms_image: 'image',
    aps: 'ios',
    // android: 'android',
  };

  /**
   * a = new PushdyNotification({title: 1, body: "test"})
   */
  constructor(data: PushdyNotificationData) {
    console.log('{PushdyNotification.constructor} data: ', data);
    if (data) {
      const keys = Object.keys(data);
      for (let i = 0, c = keys.length; i < c; i++) {
        const k = keys[i];
        if (k !== undefined) {
          const v = data[k as keyof PushdyNotificationData];
          const mappedKey = this._KeyAlias[k] || k;

          // Use type assertion to avoid indexing errors
          (this as any)[mappedKey] = v;
        }
      }

      // Map some special case
      if (isIos) {
        // restore for ios
        const aps = data.aps ? data.aps : {};
        const aps_alert = aps.alert ? aps.alert : {};
        this.title = aps_alert.title || null;
        this.body = aps_alert.body || null;
      } else if (isAndroid) {
        // restore for android
        // Android is special
        const d = data.data;
        if (d) {
          this.id = d._notification_id || null;
          this.image = d._nms_image || null;
        }
      }
    } else {
      console.error('[PushdyNotification] data is null');
    }
  }
}

const RNPushdyWrapperInst = new RNPushdyWrapper();

export function multiply(a: number, b: number): number {
  return RnPushdy.multiply(a, b);
}

export default RNPushdyWrapperInst;
