package app.vikey.ime

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/** One short, crisp tick per key (predefined TICK effect), or the system key click when haptics are off. */
class KeyHaptics(private val ctx: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31)
        (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    else @Suppress("DEPRECATION") ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val tick: VibrationEffect? = if (Build.VERSION.SDK_INT >= 29) VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK) else null
    private val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun tick(view: View) {
        val v = vibrator
        if (v != null && v.hasVibrator() && tick != null) v.vibrate(tick)
        else view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
    }
    fun click() { audio?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD) }
}
