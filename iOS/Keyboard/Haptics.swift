import CoreHaptics
import UIKit

/// One short, crisp tick per key. A Core Haptics engine is started when the keyboard opens and kept
/// running, so each tick plays right away: UIImpactFeedbackGenerator wakes the vibration motor lazily,
/// which made ticks land late and sometimes feel doubled (Tony, build 16-17). Falls back to it when
/// Core Haptics isn't available.
final class KeyHaptics {
    private var engine: CHHapticEngine?
    private var player: CHHapticPatternPlayer?
    private let fallback = UIImpactFeedbackGenerator(style: .light)
    private var running = false

    func start() {
        guard CHHapticEngine.capabilitiesForHardware().supportsHaptics else { fallback.prepare(); return }
        do {
            if engine == nil {
                let e = try CHHapticEngine()
                e.playsHapticsOnly = true
                e.isAutoShutdownEnabled = false
                e.resetHandler = { [weak self] in self?.running = false; self?.player = nil; self?.start() }
                e.stoppedHandler = { [weak self] _ in self?.running = false }
                engine = e
            }
            if !running { try engine?.start(); running = true }
            if player == nil {
                let tick = CHHapticEvent(eventType: .hapticTransient, parameters: [
                    CHHapticEventParameter(parameterID: .hapticIntensity, value: 0.4),
                    CHHapticEventParameter(parameterID: .hapticSharpness, value: 0.55)], relativeTime: 0)
                player = try engine?.makePlayer(with: CHHapticPattern(events: [tick], parameters: []))
            }
        } catch {
            engine = nil; player = nil; running = false
            fallback.prepare()
        }
    }

    func stop() { engine?.stop(completionHandler: nil); running = false }

    func tick() {
        if running, let p = player, (try? p.start(atTime: CHHapticTimeImmediate)) != nil { return }
        fallback.impactOccurred(intensity: 0.75)
        fallback.prepare()
    }
}
