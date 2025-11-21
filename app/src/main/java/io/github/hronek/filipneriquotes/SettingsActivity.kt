package io.github.hronek.filipneriquotes

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import io.github.hronek.filipneriquotes.data.Prefs
import java.util.Calendar
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import io.github.hronek.filipneriquotes.notifications.NotificationScheduler
import android.app.AlarmManager
import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
        title = getString(R.string.settings_title)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private val REQ_POST_NOTIFICATIONS = 5001
        private var pendingEnableAfterPermissions = false
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val notifSwitch = findPreference<SwitchPreferenceCompat>("notif_enabled")
            val notifTimePref = findPreference<Preference>("notif_time")
            val themePref = findPreference<ListPreference>("app_theme")

            notifSwitch?.isChecked = Prefs.isNotifEnabled(requireContext())
            notifSwitch?.setOnPreferenceChangeListener { _, newValue ->
                val enable = newValue as Boolean
                if (enable) {
                    // 1) Android 13+ runtime notif permission
                    if (Build.VERSION.SDK_INT >= 33) {
                        val granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_POST_NOTIFICATIONS)
                            // do not enable yet; wait for result
                            return@setOnPreferenceChangeListener false
                        }
                    }
                    // 2) Android 12+ exact alarm capability
                    if (Build.VERSION.SDK_INT >= 31) {
                        val am = requireContext().getSystemService(AlarmManager::class.java)
                        val canExact = am.canScheduleExactAlarms()
                        if (!canExact) {
                            pendingEnableAfterPermissions = true
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            intent.data = android.net.Uri.parse("package:" + requireContext().packageName)
                            startActivity(intent)
                            return@setOnPreferenceChangeListener false
                        }
                    }
                    Prefs.setNotifEnabled(requireContext(), true)
                    val (h, m) = Prefs.getNotifTime(requireContext())
                    NotificationScheduler.scheduleDaily(requireContext(), h, m)
                } else {
                    Prefs.setNotifEnabled(requireContext(), false)
                    NotificationScheduler.cancel(requireContext())
                }
                true
            }

            fun updateTimeSummary() {
                val (h, m) = Prefs.getNotifTime(requireContext())
                notifTimePref?.summary = String.format("%02d:%02d", h, m)
            }

            updateTimeSummary()
            notifTimePref?.setOnPreferenceClickListener {
                val (h, m) = Prefs.getNotifTime(requireContext())
                TimePickerDialog(requireContext(), { _, hour, minute ->
                    Prefs.setNotifTime(requireContext(), hour, minute)
                    updateTimeSummary()
                    if (Prefs.isNotifEnabled(requireContext())) {
                        NotificationScheduler.scheduleDaily(requireContext(), hour, minute)
                    }
                }, h, m, true).show()
                true
            }

            // Theme preference
            themePref?.value = Prefs.getTheme(requireContext())
            fun applyTheme(value: String) {
                when (value) {
                    "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
            themePref?.summary = themePref?.entry
            themePref?.setOnPreferenceChangeListener { pref, newValue ->
                val v = newValue as String
                Prefs.setTheme(requireContext(), v)
                applyTheme(v)
                (pref as ListPreference).value = v
                pref.summary = pref.entry
                true
            }
            themePref?.let { applyTheme(it.value) }
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == REQ_POST_NOTIFICATIONS) {
                val notifSwitch = findPreference<SwitchPreferenceCompat>("notif_enabled")
                val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    // continue flow in onResume to also check exact alarms
                    notifSwitch?.isChecked = true
                } else {
                    Prefs.setNotifEnabled(requireContext(), false)
                    notifSwitch?.isChecked = false
                }
            }
        }

        override fun onResume() {
            super.onResume()
            val notifSwitch = findPreference<SwitchPreferenceCompat>("notif_enabled")
            if (pendingEnableAfterPermissions || (notifSwitch?.isChecked == true && Prefs.isNotifEnabled(requireContext()))) {
                // if user returned from exact alarm settings, try to enable now
                val postGranted = if (Build.VERSION.SDK_INT >= 33)
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                else true
                val exactGranted = if (Build.VERSION.SDK_INT >= 31)
                    requireContext().getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
                else true
                if (postGranted && exactGranted) {
                    Prefs.setNotifEnabled(requireContext(), true)
                    val (h, m) = Prefs.getNotifTime(requireContext())
                    NotificationScheduler.scheduleDaily(requireContext(), h, m)
                    notifSwitch?.isChecked = true
                    pendingEnableAfterPermissions = false
                }
            }
        }
    }
}
