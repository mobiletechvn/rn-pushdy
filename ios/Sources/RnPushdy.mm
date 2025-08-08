#import "RnPushdy.h"
#import <PushdySDK/PushdySDK-Swift.h>

@implementation RnPushdy
RCT_EXPORT_MODULE()

static RnPushdy *_sharedInstance = nil;
static RNPushdyDelegateHandler *delegate = nil;
static dispatch_once_t onceToken;

- (NSNumber *)multiply:(double)a b:(double)b {
    NSNumber *result = @(a * b);

    return result;
}

+ (instancetype)sharedInstance {
  return _sharedInstance;
}

- (instancetype)init {
  self = [super init];
  [self initDelegateHandler];
  if (self) {
    _sharedInstance = self;
  }
  return self;
}

- (void)emitEventWithName:(NSString *)name body:(NSDictionary *)body {
  if ([name isEqualToString:@"onNotificationOpened"]) {
    [self emitOnNotificationOpened:body];
  } else if ([name isEqualToString:@"onNotificationReceived"]) {
    [self emitOnNotificationReceived:body];
  }

  // Add other events if needed
}

- (void)initDelegateHandler {
  
  dispatch_once(&onceToken, ^{
    delegate = [[RNPushdyDelegateHandler alloc] init];
  });

  [RNPushdySDK setDelegateHandler:delegate];
}

- (void)getAllBanners:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK getAllBannersWithResolve:resolve reject:reject];
}


- (void)getApplicationIconBadgeNumber:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK getAllBannersWithResolve:resolve reject:reject];
}


- (void)getBannerData:(NSString *)bannerId resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK getBannerData:bannerId resolve:resolve reject:reject];
}


- (void)getDeviceId:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK getDeviceIdWithResolve:resolve reject:reject];
}


- (void)getDeviceToken:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK getDeviceToken:resolve reject:reject];
}


- (void)getInitialNotification:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK getInitialNotification:resolve reject:reject];
}


- (void)getPendingEvents:(double)count resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  NSNumber *countNumber = [NSNumber numberWithDouble:count];
  [RNPushdySDK getPendingEvents:countNumber resolve:resolve reject:reject];
}


- (void)getPendingNotification:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK getPendingNotificationWithResolve:resolve reject:reject];
}


- (void)getPendingNotifications:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK getPendingNotificationsWithResolve:resolve reject:reject];
}


- (void)getPlayerID:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK getPlayerID:resolve reject:reject];
}


- (void)getReadyForHandlingNotification:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  resolve(@YES);
}


- (void)handleCustomInAppBannerPressed:(NSString *)notificationId resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK handleCustomInAppBannerPressed:notificationId resolve:resolve reject:reject];
}


- (void)initPushdy:(NSDictionary *)options resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK initPushdy:options resolve:resolve reject:reject];
}


- (void)isAppOpenedFromPush:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK isAppOpenedFromPushWithResolve:resolve reject:reject];
}


- (void)isNotificationEnabled:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK isNotificationEnabledWithResolve:resolve reject:reject];
}


- (void)isRemoteNotificationRegistered:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK isRemoteNotificationRegisteredWithResolve:resolve reject:reject];
}


- (void)makeCrash:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  // Dont use anymore, just a template now. Consider to delete it later
  resolve(@YES);
}


- (void)pushAttributeArray:(NSString *)attr value:(NSArray *)value commitImmediately:(BOOL)commitImmediately resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
}


- (void)pushPendingEvents:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK pushPendingEventsWithResolve:resolve reject:reject];
}


- (void)registerForPushNotification:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK registerForPushNotificationsWithResolve:resolve reject:reject];
}


- (void)removeInitialNotification:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK removeInitialNotification:resolve reject:reject];
}


- (void)removePendingEvents:(double)count resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  NSNumber *countNumber = [NSNumber numberWithDouble:count];
  [RNPushdySDK removePendingEvents:countNumber resolve:resolve reject:reject];
}


- (void)sampleMethod:(NSString *)stringArgument numberArgument:(double)numberArgument resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  // This a template, don't do anything right now.
  resolve(@"sampleMethod");
}


- (void)setApplicationIconBadgeNumber:(double)count resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  
  NSNumber *countNumber = [NSNumber numberWithDouble:count];
  [RNPushdySDK setApplicationIconBadgeNumberWithCount:countNumber resolve:resolve reject:reject];
}


- (void)setApplicationId:(NSString *)applicationId resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK setApplicationId: applicationId resolve:resolve reject:reject];
}

- (void)setAttributeFromOption:(JS::NativeRnPushdy::SpecSetAttributeFromOptionOptions &)options resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
}


- (void)setAttributeFromValueContainer:(NSString *)attr valueContainer:(JS::NativeRnPushdy::SpecSetAttributeFromValueContainerValueContainer &)valueContainer commitImmediately:(BOOL)commitImmediately resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  
  id<NSObject> data = valueContainer.data();
  
  NSDictionary *dict = @{
    @"data": data
  };
  
  [RNPushdySDK setAttributeFromValueContainerWithAttr:attr valueContainer:dict commitImmediately:commitImmediately resolve:resolve reject:reject];
}


- (void)setBadgeOnForeground:(BOOL)enable resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  resolve(@YES);
}


- (void)setCustomMediaKey:(NSString *)mediaKey resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK setCustomMediaKeyWithMediaKey:mediaKey resolve:resolve reject:reject];
}


- (void)setCustomPushBanner:(NSString *)viewType resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK setCustomPushBannerWithViewType:viewType resolve:resolve reject:reject];
}


- (void)setDeviceId:(NSString *)deviceId resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK setDeviceIdWithId:deviceId resolve:resolve reject:reject];
}


- (void)setPendingEvents:(NSArray *)events resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK setPendingEvents:events resolve:resolve reject:reject];
}


- (void)setPushBannerAutoDismiss:(BOOL)autoDismiss resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK setPushBannerAutoDismissWithAutoDismiss:autoDismiss resolve:resolve reject:reject];
}


- (void)setPushBannerDismissDuration:(double)sec resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  NSNumber *secNumber = [NSNumber numberWithDouble:sec];
  [RNPushdySDK setPushBannerDismissDurationWithSeconds:secNumber resolve:resolve reject:reject];
}


- (void)setSubscribedEvents:(NSArray *)subscribedEventNames resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  resolve(@"deprecated");
}


- (void)startHandleIncommingNotification:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  resolve(@YES);
}


- (void)stopHandleIncommingNotification:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  resolve(@YES);
}


- (void)subscribe:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK subscribeWithResolve:resolve reject:reject];
}


- (void)trackBanner:(NSString *)bannerId type:(NSString *)type resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK trackBanner:bannerId type:type resolve:resolve reject:reject];
}


- (void)trackEvent:(NSString *)eventName eventProperties:(NSDictionary *)eventProperties immediate:(BOOL)immediate resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK trackEvent:eventName params:eventProperties immediate:immediate resolve:resolve reject:reject];
}


- (void)useSDKHandler:(BOOL)enabled resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  [RNPushdySDK useSDKHandler:enabled resolve:resolve reject:reject];
}


- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeRnPushdySpecJSI>(params);
}

@end


@implementation RNPushdyDelegateHandler

- (void)sendEventToJsWithEventName:(NSString *)eventName body:(NSDictionary<NSString *, id> *)body {
  NSLog(@"Sending event to JS: %@, body: %@", eventName, body);
  [[RnPushdy sharedInstance] emitEventWithName:eventName body:body];
}

- (void)onNotificationOpened:(NSDictionary<NSString *,id> *)notification fromState:(NSString *)fromState {
  NSLog(@"[Pushdy] Notification opened from state: %@", fromState);
  [RNPushdySDK onNotificationOpened:notification fromState:fromState];
  
  NSDictionary *universalNotification = [RNPushdySDK toRNPushdyStructureObjcC:notification];
      [self sendEventToJsWithEventName:@"onNotificationOpened" body:@{
          @"notification": universalNotification,
          @"fromState": fromState
      }];
}

- (void)onNotificationReceived:(NSDictionary<NSString *,id> *)notification fromState:(NSString *)fromState {
  NSLog(@"[Pushdy] Notification received from state: %@", fromState);
  
  NSDictionary *universalNotification = [RNPushdySDK toRNPushdyStructureObjcC:notification];
      [self sendEventToJsWithEventName:@"onNotificationReceived" body:@{
          @"notification": universalNotification,
          @"fromState": fromState
      }];
}

- (BOOL)readyForHandlingNotification {
  return YES;
}

- (void)onRemoteNotificationRegistered:(NSString *)deviceToken {
  NSLog(@"[Pushdy] Device token registered: %@", deviceToken);
  [self sendEventToJsWithEventName:@"onRemoteNotificationRegistered" body:@{ @"deviceToken": deviceToken }];
}

- (void)onRemoteNotificationFailedToRegister:(NSError *)error {
  NSLog(@"[Pushdy] Failed to register device token: %@", error);
  [self sendEventToJsWithEventName:@"onRemoteNotificationFailedToRegister" body:@{ @"error": error.localizedDescription }];
}

@end
