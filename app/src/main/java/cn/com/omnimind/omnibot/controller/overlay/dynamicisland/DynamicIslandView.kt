package cn.com.omnimind.omnibot.controller.overlay.dynamicisland

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import cn.com.omnimind.omnibot.R
import cn.com.omnimind.omnibot.controller.overlay.dynamicisland.model.DialogueAction
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.min

@SuppressLint("ViewConstructor")
class DynamicIslandView(
    context: Context,
    private val onUserSwiped: (userSwiped: Boolean) -> Unit,
    private val onUserInteractionStarted: () -> Unit,
    private val onUserInteractionEnded: () -> Unit,
) : FrameLayout(context) {
    private val titleTextView: TextView
    private val contentTextView: TextView
    private val actionsScroller: View
    private val actionsContainer: LinearLayout
    private val gestureDetector: GestureDetector

    private var initialTouchY: Float = 0f
    private val maxDragDistancePx: Int

    private var returnToPositionAnimation: SpringAnimation? = null

    companion object {
        private const val MAX_DRAG_DISTANCE_DP = 80
        private const val DRAG_LOG_SCALE_FACTOR = 200f
        private const val DISMISS_DRAG_THRESHOLD_RATIO = 0.4f

        private const val SPRING_STIFFNESS_IN = SpringForce.STIFFNESS_LOW
        private const val SPRING_DAMPING_RATIO_IN = SpringForce.DAMPING_RATIO_LOW_BOUNCY

        private const val SPRING_STIFFNESS_OUT = SpringForce.STIFFNESS_LOW
        private const val SPRING_DAMPING_RATIO_OUT = SpringForce.DAMPING_RATIO_NO_BOUNCY
    }

    init {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dynamic_island_layout, this, true)

        titleTextView = view.findViewById(R.id.title_text)
        contentTextView = view.findViewById(R.id.content_text)
        actionsScroller = view.findViewById(R.id.actions_scroller)
        actionsContainer = view.findViewById(R.id.actions_container)

        maxDragDistancePx = (MAX_DRAG_DISTANCE_DP * resources.displayMetrics.density).toInt()
        setPadding(paddingLeft, paddingTop, paddingRight, maxDragDistancePx)
        clipToPadding = false

        gestureDetector =
            GestureDetector(
                context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onFling(
                        e1: MotionEvent?,
                        e2: MotionEvent,
                        velocityX: Float,
                        velocityY: Float,
                    ): Boolean {
                        if (e1 != null && e1.y > e2.y && abs(velocityY) > 200) {
                            onUserSwiped(true)
                            return true
                        }
                        return false
                    }
                },
            )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            returnToPositionAnimation?.cancel()
        }

        if (gestureDetector.onTouchEvent(event)) {
            return true
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                onUserInteractionStarted()
                initialTouchY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = event.rawY - initialTouchY
                if (deltaY > 0) {
                    translationY = calculateNonLinearTranslation(deltaY)
                } else {
                    translationY = deltaY
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onUserInteractionEnded()
                val finalTranslationY = translationY
                val visibleHeight = height - maxDragDistancePx
                val dismissThreshold = -visibleHeight * DISMISS_DRAG_THRESHOLD_RATIO

                if (finalTranslationY < dismissThreshold) {
                    onUserSwiped(true)
                } else {
                    returnToPositionAnimation =
                        createSpringAnimation(
                            targetValue = 0f,
                            stiffness = SpringForce.STIFFNESS_LOW,
                            dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY,
                        )
                    returnToPositionAnimation?.start()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun calculateNonLinearTranslation(deltaY: Float): Float {
        val cappedDelta = min(deltaY, maxDragDistancePx.toFloat() * 4)
        val translation = DRAG_LOG_SCALE_FACTOR * log10(cappedDelta / DRAG_LOG_SCALE_FACTOR + 1f)
        return min(translation, maxDragDistancePx.toFloat())
    }

    fun configure(
        title: String,
        content: String,
    ) {
        titleTextView.text = title
        contentTextView.text = content
        actionsScroller.visibility = GONE
    }

    fun configure(
        title: String,
        content: String,
        actions: List<DialogueAction>,
        onActionClicked: (DialogueAction) -> Unit,
    ) {
        titleTextView.text = title
        contentTextView.text = content

        actionsContainer.removeAllViews()
        if (actions.isNotEmpty()) {
            actions.forEach { action ->
                val actionView = createActionView(action, onActionClicked)
                actionsContainer.addView(actionView)
            }
            actionsScroller.visibility = VISIBLE
        } else {
            actionsScroller.visibility = GONE
        }
    }

    private fun createActionView(
        action: DialogueAction,
        onActionClicked: (DialogueAction) -> Unit,
    ): TextView {
        val actionView =
            LayoutInflater.from(context).inflate(
                R.layout.dynamic_island_dialogue_option,
                actionsContainer,
                false,
            ) as TextView

        actionView.text = action.text
        actionView.setOnClickListener {
            onActionClicked(action)
        }

        return actionView
    }

    fun animateIn(onComplete: () -> Unit) {
        if (height == 0) {
            post { animateIn(onComplete) }
            return
        }

        val startY = -(height - maxDragDistancePx).toFloat()
        translationY = startY

        val springAnim = createSpringAnimation(0f, SPRING_STIFFNESS_IN, SPRING_DAMPING_RATIO_IN)
        springAnim.addEndListener(
            object : DynamicAnimation.OnAnimationEndListener {
                override fun onAnimationEnd(
                    animation: DynamicAnimation<out DynamicAnimation<*>>?,
                    canceled: Boolean,
                    value: Float,
                    velocity: Float,
                ) {
                    animation?.removeEndListener(this)
                    if (!canceled) {
                        onComplete()
                    }
                }
            },
        )
        springAnim.start()
    }

    fun animateOut(onComplete: () -> Unit) {
        if (height == 0) {
            visibility = GONE
            onComplete()
            return
        }

        val targetY = -(height - maxDragDistancePx).toFloat()

        val springAnim = createSpringAnimation(targetY, SPRING_STIFFNESS_OUT, SPRING_DAMPING_RATIO_OUT)
        springAnim.addEndListener(
            object : DynamicAnimation.OnAnimationEndListener {
                override fun onAnimationEnd(
                    animation: DynamicAnimation<out DynamicAnimation<*>>?,
                    canceled: Boolean,
                    value: Float,
                    velocity: Float,
                ) {
                    animation?.removeEndListener(this)
                    if (!canceled) {
                        visibility = GONE
                        onComplete()
                    }
                }
            },
        )
        springAnim.start()
    }

    /**
     * Helper function to create and configure a SpringAnimation for TranslationY.
     */
    private fun createSpringAnimation(
        targetValue: Float,
        stiffness: Float,
        dampingRatio: Float,
    ): SpringAnimation {
        val animation = SpringAnimation(this, DynamicAnimation.TRANSLATION_Y)
        val spring = SpringForce()
        spring.finalPosition = targetValue
        spring.stiffness = stiffness
        spring.dampingRatio = dampingRatio
        animation.spring = spring
        return animation
    }
}
