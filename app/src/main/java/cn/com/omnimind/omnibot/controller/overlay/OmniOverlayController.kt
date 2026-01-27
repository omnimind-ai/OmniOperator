package cn.com.omnimind.omnibot.controller.overlay

import cn.com.omnimind.omnibot.OmniOperatorService
import cn.com.omnimind.omnibot.api.CoordinateScrollDirection
import cn.com.omnimind.omnibot.controller.overlay.dynamicisland.DynamicIsland
import cn.com.omnimind.omnibot.controller.overlay.dynamicisland.model.DialogueAction
import cn.com.omnimind.omnibot.controller.overlay.indicator.BaseIndicator
import cn.com.omnimind.omnibot.controller.overlay.indicator.ClickIndicator
import cn.com.omnimind.omnibot.controller.overlay.indicator.ScrollIndicator
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture

class OmniOverlayController(
    private val service: OmniOperatorService,
) {
    companion object {
        private const val TAG = "OmniOverlayController"
    }

    private var currentIndicator: BaseIndicator? = null
    private val dynamicIsland by lazy {
        DynamicIsland(service)
    }

    fun showDynamicIslandMessage(
        title: String,
        content: String,
    ): CompletableFuture<Void?> {
        return dynamicIsland.showMessage(title, content)
    }

    fun showDynamicIslandDialogue(
        title: String,
        content: String,
        actions: List<DialogueAction>,
    ): CompletableFuture<DialogueAction?> {
        return dynamicIsland.showDialogue(title, content, actions)
    }

    /**
     * Dismisses the Dynamic Island and blocks until the dismissal is complete.
     */
    suspend fun dismissDynamicIsland() {
        dynamicIsland.dismiss().await()
    }

    suspend fun showClickIndicator(
        x: Float,
        y: Float,
    ) {
        currentIndicator?.dismiss()

        val indicator = ClickIndicator(service, x, y)
        currentIndicator = indicator

        try {
            indicator.show()
        } finally {
            if (currentIndicator == indicator) {
                currentIndicator = null
            }
        }
    }

    suspend fun showScrollIndicator(
        x: Float,
        y: Float,
        scrollDirection: CoordinateScrollDirection,
        distance: Float,
    ) {
        currentIndicator?.dismiss()

        val indicator = ScrollIndicator(service, x, y, scrollDirection, distance)
        currentIndicator = indicator

        try {
            indicator.show()
        } finally {
            if (currentIndicator == indicator) {
                currentIndicator = null
            }
        }
    }

    /**
     * Cleans up all overlays, including indicators and the Dynamic Island.
     */
    fun cleanUp() {
        // Clean up transient indicators
        currentIndicator?.dismiss()
        currentIndicator = null

        // Clean up the Dynamic Island (fire-and-forget, no need to wait here)
        dynamicIsland.dismiss()
        dynamicIsland.destroy()
    }
}
