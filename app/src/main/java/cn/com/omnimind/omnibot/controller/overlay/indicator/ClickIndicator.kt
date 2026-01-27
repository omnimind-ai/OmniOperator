package cn.com.omnimind.omnibot.controller.overlay.indicator

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import cn.com.omnimind.omnibot.R

class ClickIndicator(
    context: Context,
    private val x: Float,
    private val y: Float,
) : BaseIndicator(context) {
    companion object {
        private const val INDICATOR_SIZE_DP = 40
        private const val OVERSHOOT_TENSION = 2.0f
        private const val POP_IN_DURATION_MS = 400L
        private const val PAUSE_DURATION_MS = 100L
        private const val FADE_OUT_DURATION_MS = 600L
    }

    private val popInInterpolator = OvershootInterpolator(OVERSHOOT_TENSION)
    private val fadeOutInterpolator = DecelerateInterpolator()

    override fun showInternal(onFinished: () -> Unit) {
        val density = context.resources.displayMetrics.density
        val finalIndicatorSizePx = (INDICATOR_SIZE_DP * density).toInt()

        val containerSizePx = (finalIndicatorSizePx * 1.5f).toInt()

        val childImageView =
            ImageView(context).apply {
                setImageResource(R.drawable.click_indicator_background)
                layoutParams =
                    FrameLayout.LayoutParams(
                        finalIndicatorSizePx,
                        finalIndicatorSizePx,
                        Gravity.CENTER,
                    )
            }

        val containerView =
            FrameLayout(context).apply {
                addView(childImageView)
            }
        indicatorView = containerView

        val params =
            WindowManager.LayoutParams(
                containerSizePx,
                containerSizePx,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                this.x = (this@ClickIndicator.x - containerSizePx / 2).toInt()
                this.y = (this@ClickIndicator.y - containerSizePx / 2).toInt() - statusBarHeight
            }

        windowManager.addView(containerView, params)

        childImageView.alpha = 0f
        childImageView.scaleX = 0.5f
        childImageView.scaleY = 0.5f

        childImageView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(POP_IN_DURATION_MS)
            .setInterpolator(popInInterpolator)
            .withEndAction {
                childImageView.animate()
                    .alpha(0f)
                    .setDuration(FADE_OUT_DURATION_MS)
                    .setStartDelay(PAUSE_DURATION_MS)
                    .setInterpolator(fadeOutInterpolator)
                    .withEndAction {
                        cleanup()
                        onFinished()
                    }
                    .start()
            }
            .start()
    }
}
