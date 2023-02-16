package com.wilinz.accessbilityx.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.os.Build
import android.util.Log

const val TAG = "App.kt"

fun Context.launchAppPackage(packageName: String): Boolean {
    return try {
        startActivity(
            packageManager.getLaunchIntentForPackage(packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun Context.launchAppByName(name: String): Boolean {
    val packageName = getPackageName(name)
    return packageName?.let {
        launchAppPackage(it)
    } ?: false
}

@SuppressLint("QueryPermissionsNeeded")
fun Context.getPackageName(appName: String): String? {
    val installedApplications =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        } else {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        }

    return installedApplications.firstOrNull {
        packageManager.getApplicationLabel(it).toString() == appName
    }?.packageName

}
