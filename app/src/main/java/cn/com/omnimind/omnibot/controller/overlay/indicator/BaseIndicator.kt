package cn.com.omnimind.omnibot.controller.overlay.indicator

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

abstract class BaseIndicator(protected val context: Context) {
    protected val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    protected val mainThreadHandler = Handler(Looper.getMainLooper())
    protected var indicatorView: View? = null

    /**
     * Shows the indicator and suspends the coroutine until the indicator's
     * animation is complete. Handles cancellation automatically.
     */
    suspend fun show() {
        suspendCancellableCoroutine<Unit> { continuation ->
            continuation.invokeOnCancellation {
                dismiss()
            }

            mainThreadHandler.post {
                cleanup()
                showInternal {
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
            }
        }
    }

    fun dismiss() {
        mainThreadHandler.post {
            cleanup()
        }
    }

    internal fun cleanup() {
        indicatorView?.let {
            if (it.isAttachedToWindow) {
                windowManager.removeViewImmediate(it)
            }
        }
        indicatorView = null
    }

    protected val statusBarHeight: Int by lazy {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    /**
     * Subclasses implement this to create the view and run the animation.
     * @param onComplete The callback that *must* be invoked when the indicator is done.
     */
    protected abstract fun showInternal(onComplete: () -> Unit)
}
