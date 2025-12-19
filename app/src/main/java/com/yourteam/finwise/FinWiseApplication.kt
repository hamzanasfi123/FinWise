package com.yourteam.finwise

import android.app.Application
import com.yourteam.finwise.utils.NotificationPermissionHelper
import com.yourteam.finwise.utils.ThemeUtils

class FinWiseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ThemeUtils.initializeTheme(this)
    }
    private fun checkNotificationPermission() {
        if (!NotificationPermissionHelper.hasNotificationPermission(this)) {
            // We'll request permission from MainActivity instead
            // to ensure we have an Activity context
        }
    }
}