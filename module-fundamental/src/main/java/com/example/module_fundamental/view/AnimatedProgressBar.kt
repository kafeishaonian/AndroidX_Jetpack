package com.example.module_fundamental.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ProgressBar
import androidx.core.view.isVisible

class AnimatedProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ProgressBar(context, attrs, defStyleAttr) {

    private val TAG = "AnimatedProgressBar"
    // 零进度动画
    private var idleAnimator: ValueAnimator? = null
    // 进度更新动画
    private var progressAnimator: ValueAnimator? = null
    private var animatedPosition = 0f
    private var animatedWidth = dpToPx(5f)

    // 双进度系统 (实际进度 + 动画进度)
    private var targetProgress = 0f
    private var animatedProgress = 0f

    // 初始化动画的最大最小宽度
    private var minIndicatorMaxWidth = dpToPx(90f)
    private val minIndicatorMinWidth = dpToPx(12f)
    private var cornerRadius = 0f

    // 进度条颜色
    private val inactiveColor = Color.parseColor("#E2E8EC")
    private val activeColor = Color.parseColor("#D74023")
    private val completedColor = Color.parseColor("#D74023")

    private val path = Path()
    private val rect = RectF()
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inactiveColor
        style = Paint.Style.FILL
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // 获取View的最大宽高
    private var measuredWidth = 0
    private var measuredHeight = 0
    private var animationPending = false

    init {
        progressDrawable = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            defaultFocusHighlightEnabled = false
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        measuredHeight = MeasureSpec.getSize(heightMeasureSpec)

        cornerRadius = measuredHeight / 2f

        Log.d(TAG, "Measured dimensions: $measuredWidth x $measuredHeight, radius=$cornerRadius")

        if (animationPending && measuredWidth > 0) {
            Log.d(TAG, "Executing pending animation")
            animationPending = false

            minIndicatorMaxWidth = (measuredWidth / 3.5).toFloat()
            Log.d(TAG, "minIndicatorMaxWidth:= $minIndicatorMaxWidth")

            // 根据当前状态启动不同动画
            if (targetProgress == 0f) {
                startIdleAnimation()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, "Attached to window")
        tryStartAnimationsWithSizeCheck()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        Log.d(TAG, "Visibility changed to: $visibility")

        when (visibility) {
            VISIBLE -> tryStartAnimationsWithSizeCheck()
            else -> stopAllAnimations()
        }
    }

    override fun onDetachedFromWindow() {
        Log.d(TAG, "Detached from window")
        stopAllAnimations()
        super.onDetachedFromWindow()
    }

    private fun tryStartAnimationsWithSizeCheck() {
        Log.d(TAG, "tryStartAnimations: target=$targetProgress, visible=$isVisible")

        if (visibility != VISIBLE) return

        when {
            measuredWidth > 0 -> {
                if (targetProgress == 0f) {
                    Log.d(TAG, "Starting idle animation immediately")
                    startIdleAnimation()
                }
            }
            else -> {
                Log.w(TAG, "Width not available, marking animation pending")
                animationPending = true
                requestLayout()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (measuredWidth <= 0 || measuredHeight <= 0) {
            Log.w(TAG, "Skipping draw - invalid dimensions")
            return
        }

        // 绘制背景
        rect.set(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint)

        when {
            // 优先绘制活跃进度
            animatedProgress > 0 -> drawAnimatedProgress(canvas)
            // 当进度为0时绘制指示器
            idleAnimator?.isRunning == true -> drawActiveIndicator(canvas)
        }
    }

    private fun startIdleAnimation() {
        if (idleAnimator?.isRunning == true) {
            Log.d(TAG, "Idle animation already running")
            return
        }

        if (measuredWidth <= 0) {
            Log.w(TAG, "Cannot start idle animation - measuredWidth is $measuredWidth")
            animationPending = true
            requestLayout()
            return
        }

        Log.d(TAG, "Starting idle animation with width=$measuredWidth")

        idleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE

            addUpdateListener { anim ->
                val fraction = anim.animatedFraction
                val widthFloat = measuredWidth.toFloat()

                when {
                    fraction < 0.25f -> {
                        val phaseFraction = fraction / 0.25f
                        animatedPosition = widthFloat * phaseFraction * 0.25f
                        animatedWidth = minIndicatorMinWidth +
                                (minIndicatorMaxWidth - minIndicatorMinWidth) * phaseFraction
                    }
                    fraction < 0.75f -> {
                        val phaseFraction = (fraction - 0.25f) / 0.5f
                        animatedPosition = widthFloat * 0.25f + (widthFloat * 0.75f * phaseFraction)
                        animatedWidth = minIndicatorMaxWidth
                    }
                    else -> {
                        val phaseFraction = (fraction - 0.75f) / 0.25f
                        animatedPosition = widthFloat
                        animatedWidth = minIndicatorMaxWidth -
                                (minIndicatorMaxWidth - minIndicatorMinWidth) * phaseFraction
                    }
                }
                invalidate()
                Log.v(TAG, "Idle animation: pos=$animatedPosition, width=$animatedWidth")
            }
        }
        idleAnimator?.start()
    }

    private fun startProgressAnimation(newTarget: Int) {
        Log.d(TAG, "Starting progress animation from ${animatedProgress.toInt()} to $newTarget")

        // 停止零进度动画
        idleAnimator?.cancel()

        progressAnimator?.cancel()

        val start = animatedProgress
        val end = newTarget.toFloat()
        targetProgress = end

        progressAnimator = ValueAnimator.ofFloat(start, end).apply {
            duration = 300 // 平滑动画300ms
            interpolator = null // 使用线性插值器

            addUpdateListener { anim ->
                animatedProgress = anim.animatedValue as Float
                Log.v(TAG, "Progress update: $animatedProgress")
                invalidate()
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // 当动画结束且回到零进度时，启动零进度动画
                    if (newTarget == 0) {
                        startIdleAnimation()
                    }
                }
            })

            start()
        }
    }

    private fun stopAllAnimations() {
        idleAnimator?.cancel()
        progressAnimator?.cancel()
        idleAnimator = null
        progressAnimator = null
        animationPending = false
    }

    private fun drawActiveIndicator(canvas: Canvas) {
        progressPaint.color = activeColor
        val maxWidth = measuredWidth.toFloat()

        val left = (animatedPosition - animatedWidth).coerceAtLeast(0f)
        val right = animatedPosition.coerceAtMost(maxWidth)

        if (right - left < minIndicatorMinWidth) return

        rect.set(left, 0f, right, measuredHeight.toFloat())

        path.reset()
        val radii = FloatArray(8) { cornerRadius }
        path.addRoundRect(rect, radii, Path.Direction.CW)
        canvas.drawPath(path, progressPaint)
    }

    private fun drawAnimatedProgress(canvas: Canvas) {
        val progressWidth = (animatedProgress / max * measuredWidth).coerceIn(0f, measuredWidth.toFloat())

        val colorFraction = (animatedProgress / max * 0.7f).coerceIn(0f, 1f)
        progressPaint.color = blendColors(activeColor, completedColor, colorFraction)

        rect.set(0f, 0f, progressWidth, measuredHeight.toFloat())

        path.reset()
        val radii = FloatArray(8) { cornerRadius }
        path.addRoundRect(rect, radii, Path.Direction.CW)
        canvas.drawPath(path, progressPaint)
    }

    // 优化后的进度设置方法，支持动画过渡
    override fun setProgress(progress: Int) {
        val newProgress = progress.coerceIn(0, max)

        if (newProgress == targetProgress.toInt()) {
            Log.d(TAG, "Ignore same progress: $progress")
            return
        }

        Log.d(TAG, "Set progress: $progress (previous target: $targetProgress)")

        // 停止零进度动画（如果有）
        if (idleAnimator?.isRunning == true) {
            idleAnimator?.cancel()
        }

        // 如果视图可见且尺寸已测量，直接启动动画
        if (isVisible && measuredWidth > 0) {
            startProgressAnimation(newProgress)
        } else {
            // 否则设置目标值并在后续绘制
            targetProgress = newProgress.toFloat()
            animatedProgress = targetProgress
            animationPending = false
        }

        // 调用父类更新实际值
        super.setProgress(newProgress)
    }

    override fun setMax(max: Int) {
        super.setMax(max)
        // 确保进度不超过新最大值
        targetProgress = targetProgress.coerceAtMost(max.toFloat())
        animatedProgress = animatedProgress.coerceAtMost(max.toFloat())
    }

    // 直接设置进度不带动画（用于初始化）
    fun setProgressImmediately(progress: Int) {
        val newProgress = progress.coerceIn(0, max)
        Log.d(TAG, "Set progress immediately: $newProgress")

        stopAllAnimations()
        targetProgress = newProgress.toFloat()
        animatedProgress = targetProgress
        super.setProgress(newProgress)
        invalidate()
    }

    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val r = Color.red(color1) * (1 - ratio) + Color.red(color2) * ratio
        val g = Color.green(color1) * (1 - ratio) + Color.green(color2) * ratio
        val b = Color.blue(color1) * (1 - ratio) + Color.blue(color2) * ratio
        return Color.rgb(r.toInt(), g.toInt(), b.toInt())
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}