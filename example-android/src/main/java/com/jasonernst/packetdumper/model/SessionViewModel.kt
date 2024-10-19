package com.jasonernst.packetdumper.model
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.jasonernst.packetdumper.Session

class SessionViewModel private constructor(private val sharedPreferences: SharedPreferences): ViewModel() {
    val sessionMap = mutableStateMapOf<String, Session>()
    private val _hidePermissionsScreen = mutableStateOf(sharedPreferences.getBoolean(HIDE_PERMISSION_SCREEN_KEY, false))
    private val _serviceStarted = mutableStateOf(false)
    private val _pcapServerStarted = mutableStateOf(false)
    private val _pcapUsers = mutableStateListOf<String>()

    companion object {
        private var instance: SessionViewModel? = null
        const val HIDE_PERMISSION_SCREEN_KEY = "HIDE_PERMISSION_SCREEN"

        fun getInstance(sharedPreferences: SharedPreferences): SessionViewModel {
            if (instance == null) {
                instance = SessionViewModel(sharedPreferences)
            }
            return instance as SessionViewModel
        }
    }

    fun isPermissionScreenHidden(): Boolean {
        return _hidePermissionsScreen.value
    }

    fun hidePermissionScreen() {
        _hidePermissionsScreen.value = true
        sharedPreferences.edit().putBoolean(HIDE_PERMISSION_SCREEN_KEY, true).apply()
    }

    fun showPermissionScreen() {
        _hidePermissionsScreen.value = false
        sharedPreferences.edit().putBoolean(HIDE_PERMISSION_SCREEN_KEY, false).apply()
    }

    fun serviceStarted() {
        _serviceStarted.value = true
    }

    fun serviceStopped() {
        _serviceStarted.value = false
    }

    fun isServiceStarted(): Boolean {
        return _serviceStarted.value
    }

    fun pcapServerStarted() {
        _pcapServerStarted.value = true
    }

    fun pcapServerStopped() {
        _pcapServerStarted.value = false
    }

    fun isPcapServerStarted(): Boolean {
        return _pcapServerStarted.value
    }

    fun pcapUsersChanged(users: List<String>) {
        _pcapUsers.clear()
        _pcapUsers.addAll(users)
    }

    fun getPcapUsers(): List<String> {
        return _pcapUsers
    }
}