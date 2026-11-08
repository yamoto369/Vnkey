import AppKit
import Sparkle

/// Updates from vikey.app/appcast.xml (Sparkle, EdDSA-signed). Checks daily; installs on its own when allowed.
final class Updater: ObservableObject {
    static let shared = Updater()
    private let controller = SPUStandardUpdaterController(startingUpdater: true, updaterDelegate: nil, userDriverDelegate: nil)
    private var updater: SPUUpdater { controller.updater }

    /// Checks and installs updates without asking.
    var automatic: Bool {
        get { updater.automaticallyChecksForUpdates && updater.automaticallyDownloadsUpdates }
        set {
            updater.automaticallyChecksForUpdates = newValue
            updater.automaticallyDownloadsUpdates = newValue
            objectWillChange.send()
        }
    }

    func checkForUpdates() {
        NSApp.activate(ignoringOtherApps: true)   // a menu bar app: bring Sparkle's window to the front
        updater.checkForUpdates()
    }
}
