import UIKit
import SwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            // Compose must fill the whole screen, including behind the status
            // bar and home indicator — otherwise SwiftUI insets it to the safe
            // area and the uncovered strip shows through as black rather than
            // #111111. TbcScaffold applies WindowInsets.safeDrawing internally,
            // so content still keeps clear of the notch.
            .ignoresSafeArea()
            // Compose runs its own keyboard handling; letting SwiftUI resize
            // the view as well would fight it.
            .ignoresSafeArea(.keyboard)
            // The app is dark-only. Without this, UIKit derives the status bar
            // style from the device's appearance setting, so a phone in light
            // mode gets dark status-bar text over the #111111 canvas — legible
            // nowhere. Compose does not control the status bar on iOS, so this
            // has to be declared on the SwiftUI side.
            .preferredColorScheme(.dark)
    }
}
