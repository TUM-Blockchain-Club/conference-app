import UIKit
import UserNotifications
import shared

/// The push plumbing SwiftUI's App lifecycle has no hooks for: the remote
/// notification token, and foreground presentation.
///
/// Everything here is inert unless push is enabled: the Kotlin side only calls
/// `registerForRemoteNotifications()` when `PushConfig.ENABLED` is true, so
/// without credentials neither token callback ever fires. Setting the
/// notification-center delegate is harmless either way.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    /// The send path speaks FCM only, and an APNs device token is not an FCM
    /// token. This forwards the raw APNs token so the wiring is exercised end
    /// to end; turning iOS push on for real also means adding the Firebase iOS
    /// SDK (FirebaseMessaging) and replacing this body with
    ///
    ///     Messaging.messaging().apnsToken = deviceToken
    ///
    /// plus a `MessagingDelegate` whose `didReceiveRegistrationToken` calls
    /// `PushEntryPointsKt.onPushTokenReceived(token:)` with the FCM token.
    /// See README.md, "Push notifications".
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        let token = deviceToken.map { String(format: "%02x", $0) }.joined()
        PushEntryPointsKt.onPushTokenReceived(token: token)
    }

    /// Expected on the simulator and on any build signed without an Apple
    /// Developer account (the aps-environment entitlement cannot be signed).
    /// Push stays off; the feed is unaffected because it never depended on it.
    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("Push registration failed: \(error.localizedDescription)")
    }

    /// iOS does not show a notification for a foreground app unless asked.
    /// An emergency notice must not go unseen by someone looking at the map.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        PushEntryPointsKt.onRemoteNotificationReceived()
        completionHandler([.banner, .list, .sound])
    }

    /// A tap on a notification: the server has something the cache may not,
    /// possibly within the feed's TTL, so refresh past it.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        PushEntryPointsKt.onRemoteNotificationReceived()
        completionHandler()
    }
}
