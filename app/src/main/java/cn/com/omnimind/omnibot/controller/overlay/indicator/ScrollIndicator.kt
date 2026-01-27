package cn.com.omnimind.omnibot.controller.overlay.indicator

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import cn.com.omnimind.omnibot.R
import cn.com.omnimind.omnibot.api.CoordinateScrollDirection

class ScrollIndicator(
    context: Context,
    private val x: Float,
    private val y: Float,
    private val scrollDirection: CoordinateScrollDirection,
    private val distance: Float,
) : BaseIndicator(context) {
    companion object {
        private const val INDICATOR_SIZE_DP = 40
        private const val OVERSHOOT_TENSION = 2.0f
        private const val POP_IN_DURATION_MS = 400L
        private const val PAUSE_DURATION_MS = 100L
        private const val SCROLL_DURATION_MS = 600L
    }

    private val scrollDistanceDp = distance / 3f
    private val popInInterpolator = OvershootInterpolator(OVERSHOOT_TENSION)

    // A custom Bézier curve for a "flick and coast" effect.
    // It accelerates very quickly and then has a long, graceful deceleration.
    // You can visualize and create your own curves at https://cubic-bezier.com/
    private val scrollInterpolator = PathInterpolator(0.4f, 0f, 0.4f, 1f)

    override fun showInternal(onFinished: () -> Unit) {
        val density = context.resources.displayMetrics.density
        val indicatorSizePx = (INDICATOR_SIZE_DP * density).toInt()
        val scrollDistancePx = (scrollDistanceDp * density)

        // Set factor 3 to ensure the indicator is large enough to be visible
        val containerSizePx = (indicatorSizePx + scrollDistancePx).toInt() * 2

        val stageView = FrameLayout(context)
        indicatorView = stageView

        val inflater = LayoutInflater.from(context)
        val indicatorCompositeView =
            inflater.inflate(R.layout.scroll_indicator_view, null).apply {
                layoutParams =
                    FrameLayout.LayoutParams(
                        indicatorSizePx,
                        indicatorSizePx,
                        Gravity.CENTER,
                    )
            }
        stageView.addView(indicatorCompositeView)

        val arrowView = indicatorCompositeView.findViewById<View>(R.id.indicator_arrow)
        arrowView.rotation =
            when (scrollDirection) {
                CoordinateScrollDirection.UP -> -90f
                CoordinateScrollDirection.DOWN -> 90f
                CoordinateScrollDirection.LEFT -> 180f
                CoordinateScrollDirection.RIGHT -> 0f
            }

        val params =
            WindowManager.LayoutParams(
                containerSizePx,
                containerSizePx,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                this.x = (this@ScrollIndicator.x - containerSizePx / 2).toInt()
                this.y = (this@ScrollIndicator.y - containerSizePx / 2).toInt() - statusBarHeight
            }

        windowManager.addView(stageView, params)

        indicatorCompositeView.alpha = 0f
        indicatorCompositeView.scaleX = 0.5f
        indicatorCompositeView.scaleY = 0.5f

        // 1. Pop-in Animation
        indicatorCompositeView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(POP_IN_DURATION_MS)
            .setInterpolator(popInInterpolator)
            .withEndAction {
                // 2. Scroll and Fade-out Animation (after a brief pause)
                indicatorCompositeView.animate()
                    .translationX(
                        when (scrollDirection) {
                            CoordinateScrollDirection.LEFT -> -scrollDistancePx
                            CoordinateScrollDirection.RIGHT -> scrollDistancePx
                            else -> 0f
                        },
                    )
                    .translationY(
                        when (scrollDirection) {
                            CoordinateScrollDirection.UP -> -scrollDistancePx
                            CoordinateScrollDirection.DOWN -> scrollDistancePx
                            else -> 0f
                        },
                    )
                    .alpha(0f)
                    .setStartDelay(PAUSE_DURATION_MS)
                    .setDuration(SCROLL_DURATION_MS)
                    .setInterpolator(scrollInterpolator)
                    .withEndAction {
                        // 3. Cleanup
                        cleanup()
                        onFinished()
                    }
                    .start()
            }
            .start()
    }
}
