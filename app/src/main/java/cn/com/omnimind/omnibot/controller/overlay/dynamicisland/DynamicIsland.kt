package cn.com.omnimind.omnibot.controller.overlay.dynamicisland

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import cn.com.omnimind.omnibot.controller.overlay.dynamicisland.model.DialogueAction
import java.util.concurrent.CompletableFuture

class DynamicIsland(
    private val context: Context,
) : ComponentCallbacks2 {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    private var currentView: DynamicIslandView? = null
    private var currentFuture: CompletableFuture<*>? = null
    private var dismissRunnable: Runnable? = null

    private var isCurrentlyDialogue: Boolean = false
    private var currentTitle: String? = null
    private var currentContent: String? = null
    private var currentActions: List<DialogueAction>? = null
    private var currentActionHandler: ((DialogueAction?) -> Unit)? = null

    val isShowing: Boolean
        get() = currentView != null

    companion object {
        private const val MESSAGE_DISPLAY_DURATION_MS = 5000L
    }

    init {
        context.registerComponentCallbacks(this)
    }

    fun showMessage(
        title: String,
        content: String,
    ): CompletableFuture<Void?> {
        val future = CompletableFuture<Void?>()

        mainThreadHandler.post {
            cleanupCurrentView() // Dismiss previous view without waiting
            currentFuture = future

            isCurrentlyDialogue = false
            currentTitle = title
            currentContent = content
            currentActions = null
            currentActionHandler = null

            val view =
                createAndShowView(
                    onUserSwiped = { cleanupCurrentView() },
                    onInteractionStarted = { pauseDismissTimer() },
                    onInteractionEnded = { resumeDismissTimer() },
                )

            view.configure(title, content)
            currentView = view

            dismissRunnable = Runnable { cleanupCurrentView() }
            resumeDismissTimer()
        }
        return future
    }

    fun showDialogue(
        title: String,
        content: String,
        actions: List<DialogueAction>,
    ): CompletableFuture<DialogueAction?> {
        val future = CompletableFuture<DialogueAction?>()

        mainThreadHandler.post {
            cleanupCurrentView() // Dismiss previous view without waiting
            currentFuture = future

            val actionHandler: (DialogueAction?) -> Unit = { chosenAction ->
                if (!future.isDone) future.complete(chosenAction)
                cleanupCurrentView()
            }

            isCurrentlyDialogue = true
            currentTitle = title
            currentContent = content
            currentActions = actions
            currentActionHandler = actionHandler

            val view =
                createAndShowView(
                    onUserSwiped = { actionHandler(null) },
                    onInteractionStarted = { /* Dialogues don't have timers, do nothing */ },
                    onInteractionEnded = { /* Dialogues don't have timers, do nothing */ },
                )

            view.configure(title, content, actions, actionHandler)
            currentView = view
        }
        return future
    }

    /**
     * Dismisses the current view and returns a CompletableFuture that completes
     * when the dismissal animation and view removal are finished.
     */
    fun dismiss(): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()
        mainThreadHandler.post {
            cleanupCurrentView().whenComplete { _, _ -> future.complete(Unit) }
        }
        return future
    }

    // This should be called from your service's onDestroy or a similar cleanup method.
    fun destroy() {
        context.unregisterComponentCallbacks(this)
        mainThreadHandler.post { cleanupCurrentView() }
    }

    private fun pauseDismissTimer() {
        dismissRunnable?.let { mainThreadHandler.removeCallbacks(it) }
    }

    private fun resumeDismissTimer() {
        if (!isCurrentlyDialogue) {
            dismissRunnable?.let {
                mainThreadHandler.postDelayed(it, MESSAGE_DISPLAY_DURATION_MS)
            }
        }
    }

    /**
     * Cleans up the current view and returns a future that completes upon finish.
     * This method MUST be called from the main thread.
     */
    private fun cleanupCurrentView(): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()

        pauseDismissTimer()
        dismissRunnable = null

        if (currentFuture?.isDone == false) {
            (currentFuture as? CompletableFuture<DialogueAction?>)?.complete(null)
            (currentFuture as? CompletableFuture<Void?>)?.complete(null)
        }
        currentFuture = null

        // Clear state
        currentTitle = null
        currentContent = null
        currentActions = null
        currentActionHandler = null

        val viewToDismiss = currentView
        currentView = null // Set to null immediately to prevent re-entry

        if (viewToDismiss == null) {
            future.complete(Unit) // Nothing to dismiss, complete immediately.
            return future
        }

        viewToDismiss.animateOut {
            if (viewToDismiss.isAttachedToWindow) {
                try {
                    windowManager.removeView(viewToDismiss)
                } catch (e: Exception) {
                    // Ignore exceptions, view might already be removed.
                }
            }
            future.complete(Unit) // Complete after animation and removal.
        }
        return future
    }

    private fun createAndShowView(
        onUserSwiped: (userSwiped: Boolean) -> Unit,
        onInteractionStarted: () -> Unit,
        onInteractionEnded: () -> Unit,
    ): DynamicIslandView {
        val view = DynamicIslandView(context, onUserSwiped, onInteractionStarted, onInteractionEnded)

        val params =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.FILL_HORIZONTAL
            }

        windowManager.addView(view, params)
        view.post { view.animateIn {} }
        return view
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // If a view is showing, we need to recreate it.
        if (currentView != null) {
            mainThreadHandler.post {
                recreateView()
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        // Not used, but required by the interface
    }

    override fun onLowMemory() {
        // Not used, but required by the interface
    }

    private fun recreateView() {
        // We must have state if a view is visible
        if (currentTitle == null || currentContent == null) return

        val oldView = currentView
        // Immediately remove the old view without animation for a seamless transition
        if (oldView != null && oldView.isAttachedToWindow) {
            try {
                windowManager.removeView(oldView)
            } catch (e: Exception) {
                // Ignore
            }
        }

        pauseDismissTimer()

        // Re-create the view, which will now inflate the correct layout resource
        val newView =
            if (isCurrentlyDialogue) {
                val actionHandler = currentActionHandler!!
                createAndShowView(
                    onUserSwiped = { actionHandler(null) },
                    onInteractionStarted = {},
                    onInteractionEnded = {},
                ).apply {
                    configure(currentTitle!!, currentContent!!, currentActions!!, actionHandler)
                }
            } else {
                createAndShowView(
                    onUserSwiped = { cleanupCurrentView() },
                    onInteractionStarted = { pauseDismissTimer() },
                    onInteractionEnded = { resumeDismissTimer() },
                ).apply {
                    configure(currentTitle!!, currentContent!!)
                }
            }

        currentView = newView
        resumeDismissTimer() // Restart the timer if it's a message
    }
}
