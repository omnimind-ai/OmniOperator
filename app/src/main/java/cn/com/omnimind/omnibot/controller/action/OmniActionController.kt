package cn.com.omnimind.omnibot.controller.action

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import cn.com.omnimind.omnibot.OmniIME
import cn.com.omnimind.omnibot.OmniOperatorService
import cn.com.omnimind.omnibot.api.CoordinateScrollDirection
import cn.com.omnimind.omnibot.api.NodeScrollDirection
import cn.com.omnimind.omnibot.util.OmniLog
import java.util.concurrent.CompletableFuture

class OmniActionController(
    private val service: OmniOperatorService,
) {
    companion object {
        private const val TAG = "OmniGestureController"
        private const val CLICK_DURATION = 50L
        private const val LONG_CLICK_DURATION = 1000L
        private const val SCROLL_DISTANCE = 300f
        private const val SCROLL_DURATION = 500L
    }

    private val windowBounds: Rect by lazy {
        OmniLog.v(TAG, "Initializing window bounds for the first time.")
        val metrics = service.getSystemService(WindowManager::class.java).currentWindowMetrics
        metrics.bounds
    }
    private val screenWidth: Float by lazy { windowBounds.width().toFloat() }
    private val screenHeight: Float by lazy { windowBounds.height().toFloat() }

    /**
     * accessibility click action
     * if node clickable ==> performAction(ACTION_CLICK)
     * otherwise         ==> clickCoordinate
     */
    fun clickNode(node: AccessibilityNodeInfo) {
        OmniLog.v(TAG, "fun clickNode")

        if (node.isClickable) {
            if (!node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                throw RuntimeException("Perform click on node failed")
            }
        } else {
            throw RuntimeException("Node is not clickable")
        }
    }

    /**
     * accessibility long click action
     * if node clickable ==> performAction(ACTION_LONG_CLICK)
     * otherwise         ==> clickCoordinate
     */
    fun longClickNode(node: AccessibilityNodeInfo) {
        OmniLog.v(TAG, "fun longClickNode")

        if (node.isLongClickable) {
            if (!node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)) {
                throw RuntimeException("Perform long click on node failed")
            }
        } else {
            throw RuntimeException("Node is not long clickable")
        }
    }

    /**
     * accessibility scroll node action
     */
    fun scrollNode(
        node: AccessibilityNodeInfo,
        direction: NodeScrollDirection,
    ) {
        Log.d(TAG, "fun scrollNode")

        val action =
            when (direction) {
                NodeScrollDirection.FORWARD -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                NodeScrollDirection.BACKWARD -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            }

        if (node.isScrollable) {
            if (!node.performAction(action)) {
                throw RuntimeException("Scroll on node failed")
            }
        } else {
            throw RuntimeException("Node is not scrollable")
        }
    }

    /**
     * accessibility text input action
     */
    fun inputText(
        node: AccessibilityNodeInfo,
        text: String,
    ) {
        OmniLog.v(TAG, "fun inputText")

        if (node.isEditable) {
            val arguments =
                Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        text,
                    )
                }

            if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
                throw RuntimeException("Perform input text on node failed")
            }

            // TODO: Make auto-submit optional
            if (!node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)) {
                throw RuntimeException("Perform IME enter on node failed")
            }
        } else {
            throw RuntimeException("Node is not editable")
        }
    }

    /**
     * android set clipboard action
     */
    fun copyToClipboard(
        context: Context,
        text: String,
    ) {
        OmniLog.v(TAG, "fun setClipboard")
        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", text)
        clipboard.setPrimaryClip(clip)
    }

    /**
     * android inject text by Omni IME action
     */
    fun injectTextByIME(text: String) {
        OmniLog.v(TAG, "fun injectTextByIME")

        OmniIME.instance?.inputText(text)
    }

    /**
     * accessibility gesture click action
     */
    fun clickCoordinate(
        x: Float,
        y: Float,
    ): CompletableFuture<Unit> {
        OmniLog.v(TAG, "fun clickCoordinate")

        return clickCoordinateImpl(x, y, CLICK_DURATION)
    }

    fun longClickCoordinate(
        x: Float,
        y: Float,
    ): CompletableFuture<Unit> {
        OmniLog.v(TAG, "fun longClickCoordinate")

        return clickCoordinateImpl(x, y, LONG_CLICK_DURATION)
    }

    private fun clickCoordinateImpl(
        x: Float,
        y: Float,
        duration: Long,
    ): CompletableFuture<Unit> {
        val path = Path()
        path.moveTo(x, y)
        path.lineTo(x + 1f, y + 1f)

        val gestureBuilder =
            GestureDescription
                .Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        path,
                        0,
                        duration,
                    ),
                )

        val future = CompletableFuture<Unit>()

        val callback =
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    future.complete(Unit)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    future.completeExceptionally(RuntimeException("Gesture was cancelled"))
                }
            }

        val dispatchResult =
            service.dispatchGesture(
                gestureBuilder.build(),
                callback,
                null,
            )

        if (!dispatchResult) {
            future.completeExceptionally(RuntimeException("Failed to dispatch gesture"))
        }

        return future
    }

    /**
     * accessibility perform gesture scroll action
     */
    fun scrollCoordinate(
        x: Float,
        y: Float,
        direction: CoordinateScrollDirection,
        distance: Float,
    ): CompletableFuture<Unit> {
        OmniLog.v(TAG, "fun swipeCoordinate")
        OmniLog.v(TAG, "Window width: $screenWidth, height: $screenHeight")

        val (endX, endY) =
            when (direction) {
                CoordinateScrollDirection.LEFT -> Pair(maxOf(x - distance, 0f), y)
                CoordinateScrollDirection.RIGHT -> Pair(minOf(x + distance, screenWidth), y)
                CoordinateScrollDirection.UP -> Pair(x, maxOf(y - distance, 0f))
                CoordinateScrollDirection.DOWN -> Pair(x, minOf(y + distance, screenHeight))
            }

        val path =
            Path().apply {
                moveTo(x, y)
                lineTo(endX, endY)
            }

        val gestureBuilder =
            GestureDescription
                .Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        path,
                        0,
                        SCROLL_DURATION,
                    ),
                )

        val future = CompletableFuture<Unit>()

        val callback =
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    future.complete(Unit)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    future.completeExceptionally(RuntimeException("Scroll gesture cancelled"))
                }
            }

        // Dispatch gesture
        if (!service.dispatchGesture(gestureBuilder.build(), callback, null)) {
            future.completeExceptionally(RuntimeException("Failed to dispatch scroll gesture"))
        }

        return future
    }

    private fun performGlobalActionImpl(action: Int) {
        if (!service.performGlobalAction(action)) {
            throw RuntimeException("Failed to perform global action with id: $action")
        }
    }

    fun goHome() {
        OmniLog.v(TAG, "fun globalActionHome")
        performGlobalActionImpl(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    fun goBack() {
        OmniLog.v(TAG, "fun globalActionBack")
        performGlobalActionImpl(AccessibilityService.GLOBAL_ACTION_BACK)
    }

//    fun pressEnterKey() {
//        OmniLog.v(TAG, "fun pressEnterKey")
//        val now = SystemClock.uptimeMillis()
//
//
//        val downEvent: KeyEvent = KeyEvent(now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
//        dispatchKeyEvent(downEvent)
//
//
//        val upEvent: KeyEvent = KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
//        dispatchKeyEvent(upEvent)
//    }

    /**
     * accessibility launch app action
     */
    fun launchApplication(packageName: String) {
        OmniLog.v(TAG, "fun launchApplication")
        val intent = service.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            service.startActivity(intent)
        } else {
            throw RuntimeException("Application with package name $packageName not found")
        }
    }

    /**
     * get app info
     */
    fun listInstalledApplications(): Pair<List<String>, List<String>> {
        OmniLog.v(TAG, "fun listInstalledApplications")
        val packageManager = service.packageManager
        val applications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        val filteredApps =
            applications
                .filter {
                    val launchIntent = packageManager.getLaunchIntentForPackage(it.packageName)
                    launchIntent != null
                }.sortedBy { it.loadLabel(packageManager).toString() }

        val applicationNames = filteredApps.map { it.loadLabel(packageManager).toString() }
        val packageNames = filteredApps.map { it.packageName }
        OmniLog.i(TAG, "Find ${packageNames.size} app packages!")

        return Pair(packageNames, applicationNames)
    }
}
