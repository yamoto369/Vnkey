package app.vikey

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings as SysSettings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.vikey.ime.Settings
import app.vikey.ui.AcknowledgementsScreen
import app.vikey.ui.MainScreen
import app.vikey.ui.OnboardingScreen
import app.vikey.ui.VikeyTheme

/** Whether the user has enabled Vikey in Settings › Keyboards, and whether it is the current keyboard. */
object KeyboardStatus {
    fun isAdded(c: Context): Boolean {
        val imm = c.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { it.packageName == c.packageName }
    }
    fun isSelected(c: Context) = SysSettings.Secure.getString(c.contentResolver, SysSettings.Secure.DEFAULT_INPUT_METHOD)?.startsWith(c.packageName) == true
    fun openSettings(c: Context) = c.startActivity(Intent(SysSettings.ACTION_INPUT_METHOD_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    /** Settings › Apps › Vikey › Language (Android 13+). */
    fun openAppLanguage(c: Context) = c.startActivity(Intent(SysSettings.ACTION_APP_LOCALE_SETTINGS, android.net.Uri.fromParts("package", c.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun showPicker(c: Context) = (c.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = Settings.prefs(this)
        setContent {
            VikeyTheme {
                var didOnboard by remember { mutableStateOf(prefs.getBoolean(Settings.DID_ONBOARD, false)) }
                var showAck by remember { mutableStateOf(false) }
                when {
                    !didOnboard -> OnboardingScreen { prefs.edit().putBoolean(Settings.DID_ONBOARD, true).apply(); didOnboard = true }
                    showAck -> AcknowledgementsScreen { showAck = false }
                    else -> MainScreen(onReviewOnboarding = { prefs.edit().putBoolean(Settings.DID_ONBOARD, false).apply(); didOnboard = false }, onAcknowledgements = { showAck = true })
                }
            }
        }
    }
}
