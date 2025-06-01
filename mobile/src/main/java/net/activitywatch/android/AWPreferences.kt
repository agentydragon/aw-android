package net.activitywatch.android

import android.content.Context
import android.content.SharedPreferences

class AWPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("AWPreferences", Context.MODE_PRIVATE)

    companion object {
        const val SERVER_URL = "http://127.0.0.1:5600"
        const val KEY_ENABLE_LOCAL_SERVER = "enableLocalServer"
    }

    // To check if it is the first time the app is being run
    // Set to false when user finishes onboarding
    fun isFirstTime(): Boolean {
        return sharedPreferences.getBoolean("isFirstTime", true)
    }

    // To set the first time flag to false after the first run
    fun setFirstTimeRunFlag() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("isFirstTime", false)
        editor.apply()
    }

    // Optional: To reset the first time flag to true (for debugging, perhaps)
    fun resetFirstTimeRunFlag() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("isFirstTime", true)
        editor.apply()
    }

    // Server configuration - whether to start the built-in server
    fun isLocalServerEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_ENABLE_LOCAL_SERVER, true)
    }

    fun setLocalServerEnabled(enabled: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY_ENABLE_LOCAL_SERVER, enabled)
        editor.apply()
    }
}