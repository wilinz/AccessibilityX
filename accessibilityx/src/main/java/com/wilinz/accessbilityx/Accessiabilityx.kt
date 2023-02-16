package com.wilinz.accessbilityx

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.wilinz.accessbilityx.device.screenHeight
import com.wilinz.accessbilityx.device.screenWidth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val TAG = "Accessiabilityx.kt"

data class ScreenMetrics(
    val width: Int,
    val height: Int,
    val isInit: Boolean
)

open class AccessibilityxService : AccessibilityService() {

    var screenMetrics = ScreenMetrics(width = 0, height = 0, isInit = false)
        private set
    var currentScreenMetrics = ScreenMetrics(width = 0, height = 0, isInit = false)
        private set

    fun Float.translateX(isZoom: Boolean): Float {
        if (!isZoom || !screenMetrics.isInit || !currentScreenMetrics.isInit) return this
        return this * currentScreenMetrics.width / screenMetrics.width
    }

    fun Float.translateY(isZoom: Boolean): Float {
        if (!isZoom || !screenMetrics.isInit || !currentScreenMetrics.isInit) return this
        return this * currentScreenMetrics.height / screenMetrics.height
    }

    fun setScreenMetrics(width: Int, height: Int) {
        currentScreenMetrics = ScreenMetrics(screenWidth, screenHeight, true)
        screenMetrics = ScreenMetrics(width, height, true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        lastEvent = event
        MainScope().launch {
            currentPackageFlow.emit(currentPackage)
            currentActivityFlow.emit(currentActivity)
        }
        untilFindOneCoroutines.forEach {
            it.resume(Unit)
            untilFindOneCoroutines.remove(it)
        }
    }

    override fun onInterrupt() {
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
    }


    @RequiresApi(Build.VERSION_CODES.N)
    fun ensureClick(nodeInfo: AccessibilityNodeInfo): Boolean {
        if (!nodeInfo.ensureClick()) {
            return click(nodeInfo.bounds)
        }
        return false
    }


    @RequiresApi(Build.VERSION_CODES.N)
    fun ensureAccurateClick(nodeInfo: AccessibilityNodeInfo): Boolean {
        if (!nodeInfo.isClickable) {
            return click(nodeInfo.bounds)
        }
        return false
    }


    @RequiresApi(Build.VERSION_CODES.N)
    fun click(x: Float, y: Float, isZoom: Boolean = true) = press(x, y, 150, isZoom)

    @RequiresApi(Build.VERSION_CODES.N)
    fun click(bounds: Rect) =
        click(bounds.centerX().toFloat(), bounds.centerY().toFloat(), false)

    @RequiresApi(Build.VERSION_CODES.N)
    fun longClick(x: Float, y: Float, isZoom: Boolean = true) = press(x, y, 500, isZoom)

    @RequiresApi(Build.VERSION_CODES.N)
    fun press(x: Float, y: Float, duration: Long, isZoom: Boolean = true): Boolean {
        val path = Path().apply {
            moveTo(x.translateX(isZoom), y.translateY(isZoom))
        }
        val strokeDescription = GestureDescription.StrokeDescription(path, 0, duration)
        val gesture = GestureDescription.Builder().addStroke(strokeDescription).build()
        return this.dispatchGesture(gesture, null, null)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun swipe(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        duration: Long,
        isZoom: Boolean = true
    ): Boolean {
        val path = Path().apply {
            moveTo(x1.translateX(isZoom), y1.translateY(isZoom))
            lineTo(x2.translateX(isZoom), y2.translateY(isZoom))
        }
        val strokeDescription = GestureDescription.StrokeDescription(path, 0, duration)
        val gesture = GestureDescription.Builder().addStroke(strokeDescription).build()
        return this.dispatchGesture(gesture, null, null)
    }

    val rootInActiveWindow1: AccessibilityNodeInfo? get() = rootInActiveWindow
    val currentPackage get() = this.rootInActiveWindow1?.packageName1

    val currentPackageFlow get() = MutableStateFlow(currentPackage)

    var lastEvent: AccessibilityEvent? = null

    val currentActivityFlow get() = MutableStateFlow(currentActivity)

    val currentActivity: ActivityInfo?
        get() {

            fun tryGetActivity(context: Context, componentName: ComponentName): ActivityInfo? {
                return try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.packageManager.getActivityInfo(
                            componentName,
                            PackageManager.ComponentInfoFlags.of(0)
                        )
                    } else {
                        context.packageManager.getActivityInfo(componentName, 0)
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }

            val event = lastEvent ?: return null
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (event.packageName != null && event.className != null) {
                    val componentName = ComponentName(
                        event.packageName.toString(),
                        event.className.toString()
                    )
                    return tryGetActivity(this, componentName)
                }
            }
            return null
        }

    suspend fun findOnce(
        i: Int,
        predicate: (nodeInfo: AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? = withContext(Dispatchers.IO) {

        var count = 0
        var nodeInfo1: AccessibilityNodeInfo? = null
        fun recursiveFind(nodeInfo: AccessibilityNodeInfo) {
            for (j in 0 until nodeInfo.childCount) {
                if (nodeInfo1 != null && count == i) return
                val node = nodeInfo.getChild(j) ?: continue
                if (predicate(node)) {
                    if (count == i) {
                        nodeInfo1 = node
                    } else {
                        count++
                        recursiveFind(node)
                    }
                } else {
                    recursiveFind(node)
                }
            }
            return
        }

        return@withContext rootInActiveWindow1?.let { recursiveFind(it);nodeInfo1 }
    }

    suspend fun findOnce(predicate: (nodeInfo: AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? =
        withContext(Dispatchers.IO) {

            var nodeInfo1: AccessibilityNodeInfo? = null
            fun recursiveFind(nodeInfo: AccessibilityNodeInfo) {
                if (nodeInfo1 != null) return
                for (i in 0 until nodeInfo.childCount) {
                    val node = nodeInfo.getChild(i) ?: continue
                    if (predicate(node)) {
                        nodeInfo1 = node
                    } else {
                        recursiveFind(node)
                    }
                }

            }
            return@withContext rootInActiveWindow1?.let {
                recursiveFind(it)
                nodeInfo1
            }
        }

    private val untilFindOneCoroutines = CopyOnWriteArrayList<Continuation<Unit>>()

    suspend fun untilFindOne(predicate: (nodeInfo: AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo =
        withContext(Dispatchers.IO) {
            while (true) {
                val node = findOnce(predicate)
                if (node == null) {
                    suspendCoroutine {
                        untilFindOneCoroutines.add(it)
                    }
                    continue
                }
                return@withContext node
            }
            @Suppress("UNREACHABLE_CODE")
            throw Exception("UNREACHABLE_CODE")
        }

    suspend fun untilFindOne(
        timeout: Long,
        predicate: (nodeInfo: AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? = withContext(Dispatchers.IO) {
        val job: Deferred<AccessibilityNodeInfo?> = async {
            while (true) {
                val node = findOnce(predicate)
                if (node == null) {
                    suspendCoroutine {
                        untilFindOneCoroutines.add(it)
                    }
                    continue
                }
                return@async node
            }
            @Suppress("UNREACHABLE_CODE")
            return@async null
        }
        launch {
            delay(timeout)
            if (job.isActive) job.cancel()
        }
        job.await()
    }


    fun back() = performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    fun home() = performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    fun recents() = performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    fun powerDialog() =
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)

    fun notifications() =
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)

    fun quickSettings() =
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)

    @RequiresApi(Build.VERSION_CODES.N)
    fun splitScreen() =
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)

    @RequiresApi(Build.VERSION_CODES.P)
    fun takeScreenshot() =
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)

    @RequiresApi(Build.VERSION_CODES.P)
    fun lockScreen() =
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)

    @RequiresApi(Build.VERSION_CODES.S)
    fun dismissNotificationShade() =
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)

    @RequiresApi(Build.VERSION_CODES.S)
    fun keycodeHeadsetHook() =
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_KEYCODE_HEADSETHOOK)

    @RequiresApi(Build.VERSION_CODES.S)
    fun accessibilityShortcut() =
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_SHORTCUT)

    @RequiresApi(Build.VERSION_CODES.S)
    fun accessibilityButtonChooser() =
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_BUTTON_CHOOSER)

    @RequiresApi(Build.VERSION_CODES.S)
    fun accessibilityButton() =
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_BUTTON)

    @RequiresApi(Build.VERSION_CODES.S)
    fun accessibilityAllApps() =
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun dpadUp() = performGlobalAction(AccessibilityService.GLOBAL_ACTION_DPAD_UP)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun dpadDown() =
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_DPAD_DOWN)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun dpadLeft() =
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_DPAD_LEFT)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun dpadRight() =
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_DPAD_RIGHT)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun dpadCenter() =
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_DPAD_CENTER)

    companion object {
        fun isAccessibilityServiceEnabled(context: Context, clazz: Class<*>): Boolean {
            val expectedComponentName = ComponentName(context, clazz)

            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
                ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)

            while (colonSplitter.hasNext()) {
                val componentNameString = colonSplitter.next()
                val enabledService = ComponentName.unflattenFromString(componentNameString)

                if (enabledService != null && enabledService == expectedComponentName)
                    return true
            }
            return false
        }

    }
}

fun Context.goAccessibilityServiceSettings() =
    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

val AccessibilityNodeInfo.bounds
    get() :Rect {
        return Rect().apply { this@bounds.getBoundsInScreen(this) }
    }

fun AccessibilityNodeInfo.click() =
    this.performAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK.id)

fun AccessibilityNodeInfo.ensureClick(): Boolean {
    if (this.isClickable) {
        return this.click()
    } else {
        return this.parent?.ensureClick() ?: return false
    }
}

fun AccessibilityNodeInfo.longClick() =
    this.performAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK.id)

val AccessibilityNodeInfo.text1 get() = this.text?.toString()

val AccessibilityNodeInfo.packageName1 get() = this.packageName?.toString()

val AccessibilityNodeInfo.className1 get() = this.className?.toString()

val AccessibilityNodeInfo.contentDescription1 get() = this.contentDescription?.toString()

val AccessibilityNodeInfo.error1 get() = this.error?.toString()

val AccessibilityNodeInfo.hintText1
    @RequiresApi(Build.VERSION_CODES.O)
    get() = this.hintText?.toString()

val AccessibilityNodeInfo.paneTitle1
    @RequiresApi(Build.VERSION_CODES.P)
    get() = this.paneTitle?.toString()

val AccessibilityNodeInfo.stateDescription1
    @RequiresApi(Build.VERSION_CODES.R)
    get() = this.stateDescription?.toString()

val AccessibilityNodeInfo.tooltipText1
    @RequiresApi(Build.VERSION_CODES.P)
    get() = this.tooltipText?.toString()

