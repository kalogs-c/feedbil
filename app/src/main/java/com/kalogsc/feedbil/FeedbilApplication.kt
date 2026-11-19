package com.kalogsc.feedbil

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FeedbilApplication : Application() {

    lateinit var container: AppContainer
        private set

    /** Lives as long as the process; nothing needs to cancel it. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applicationScope.launch {
            runCatching { container.articleRepository.pruneAll() }
                .onFailure { Log.e(TAG, "Startup retention failed", it) }
        }
    }

    private companion object {
        const val TAG = "FeedbilApplication"
    }
}
