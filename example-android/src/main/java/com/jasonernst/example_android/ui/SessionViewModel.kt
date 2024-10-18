package com.jasonernst.example_android.ui

import androidx.lifecycle.ViewModel
import com.jasonernst.kanonproxy.Session

class SessionViewModel private constructor(): ViewModel() {
    val sessionMap = mutableMapOf<String, Session>()

    companion object {
        private var instance: SessionViewModel? = null

        fun getInstance(): SessionViewModel {
            if (instance == null) {
                instance = SessionViewModel()
            }
            return instance as SessionViewModel
        }
    }
}