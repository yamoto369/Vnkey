import SwiftUI

@main
struct VikeyApp: App {
    init() { VKSettings.register() }
    var body: some Scene {
        WindowGroup { RootView() }
    }
}

struct RootView: View {
    @AppStorage("didOnboard") private var didOnboard = false
    var body: some View {
        if didOnboard || ProcessInfo.processInfo.arguments.contains("-SkipOnboarding") {
            MainView()
        } else {
            OnboardingView { didOnboard = true }
        }
    }
}
