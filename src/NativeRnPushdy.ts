import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';
import type { EventEmitter } from 'react-native/Libraries/Types/CodegenTypes';

export interface Spec extends TurboModule {
  multiply(a: number, b: number): number;

  sampleMethod(
    stringArgument: string,
    numberArgument: number
  ): Promise<(string | number)[]>;

  makeCrash(): Promise<void>;

  initPushdy(options: { [key: string]: string }): Promise<boolean>;
  isRemoteNotificationRegistered(): Promise<boolean>;
  isAppOpenedFromPush(): Promise<boolean>;
  isNotificationEnabled(): Promise<boolean>;

  // iOS-specific methods
  registerForPushNotification(): Promise<boolean>;
  setApplicationIconBadgeNumber(count: number): Promise<void>;
  getApplicationIconBadgeNumber(): Promise<number>;

  // iOS-specific attribute setter
  setAttributeFromOption(options: {
    attr: string;
    data: Object;
    immediately: boolean;
  }): Promise<boolean>;

  startHandleIncommingNotification(): Promise<boolean>;
  stopHandleIncommingNotification(): Promise<boolean>;
  getReadyForHandlingNotification(): Promise<boolean>;

  setPushBannerAutoDismiss(autoDismiss: boolean): Promise<boolean>;
  setPushBannerDismissDuration(sec: number): Promise<boolean>;

  setCustomPushBanner(viewType: string): Promise<boolean>;
  useSDKHandler(enabled: boolean): Promise<boolean>;
  handleCustomInAppBannerPressed(notificationId: string): Promise<boolean>;

  setCustomMediaKey(mediaKey: string): Promise<boolean>;
  setDeviceId(deviceId: string): Promise<boolean>;
  getDeviceId(): Promise<string>;
  getDeviceToken(): Promise<string>;

  getPendingNotification(): Promise<{ [key: string]: Object }>;
  getPendingNotifications(): Promise<Array<{ [key: string]: Object }>>;
  getInitialNotification(): Promise<{ [key: string]: Object }>;
  removeInitialNotification(): Promise<boolean>;

  setAttributeFromValueContainer(
    attr: string,
    valueContainer: { data: Object },
    commitImmediately: boolean
  ): Promise<boolean>;

  pushAttributeArray(
    attr: string,
    value: Array<string | number | boolean>,
    commitImmediately: boolean
  ): Promise<boolean>;

  getPlayerID(): Promise<string>;

  setBadgeOnForeground(enable: boolean): Promise<boolean>;
  setSubscribedEvents(subscribedEventNames: string[]): Promise<boolean>;

  getPendingEvents(count: number): Promise<Array<{ [key: string]: Object }>>;
  setPendingEvents(events: Array<{ [key: string]: Object }>): Promise<boolean>;

  setApplicationId(applicationId: string): Promise<boolean>;
  removePendingEvents(count: number): Promise<boolean>;

  trackEvent(
    eventName: string,
    eventProperties: { [key: string]: Object },
    immediate: boolean
  ): Promise<boolean>;

  pushPendingEvents(): Promise<boolean>;
  subscribe(): Promise<boolean>;

  getAllBanners(): Promise<Array<{ [key: string]: Object }>>;
  trackBanner(bannerId: string, type: string): Promise<boolean>;
  getBannerData(bannerId: string): Promise<{ [key: string]: Object }>;

  // events
  readonly onNotificationOpened: EventEmitter<{ [key: string]: Object }>;
  readonly onNotificationReceived: EventEmitter<{ [key: string]: Object }>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('RnPushdy');
