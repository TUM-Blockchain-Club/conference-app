import SwiftUI
import shared

@main
struct iOSApp: App {
    init() {
        // The DI container belongs to the process, not to a view: started
        // here, before SwiftUI builds anything, and reused by every
        // MainViewController the scene asks for.
        _ = MainViewControllerKt.startKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
