package com.haytham.coder.curveview

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.plus

/**
 * @author Haytham Anmar Yousif
 *
 * This view used to draw custom background which support:
 * 1- curved shape
 * 2- gradient color
 * 3- shadow of different colors
 */

class CurveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val TAG = "CurvedView"
    }

    /**
     * the path to be drawn
     */
    private val path = Path()

    /**
     * the start color of gradient background
     * NOTE: use same color for start and end if u want a sold (not gradient) background
     */
    @ColorInt
    private var startColor: Int = 0

    /**
     * the end color of gradient background
     * NOTE: use same color for start and end if u want a sold (not gradient) background
     */
    @ColorInt
    private var endColor: Int = 0

    /**
     *  startColorX: the percentage of start color x position
     *  endColorX: the percentage of end color x position
     *
     *  where 0 -> LEFT of the view AND 100 -> RIGHT of the view
     *  ========================================================
     *
     *  startColorY: the percentage of start color y position
     *  endColorY: the percentage of end color y position
     *
     *  where 0 -> TOP of the view AND 100 -> BOTTOM of the view
     */
    private var startColorX: Float = 0f
    private var startColorY: Float = 0f
    private var endColorX: Float = 0f
    private var endColorY: Float = 0f

    /**
     *  firstControlPointX: the percentage of first control point x position
     *  secondControlPointX: the percentage of second control point x position
     *
     *  where 0 -> LEFT of the view AND 100 -> RIGHT of the view
     *  ========================================================
     *
     *  firstControlPointExtraY: additional height to the first control point
     *  secondControlPointExtraY: additional height to the second control point
     *
     *  where
     *  1) 0 -> TOP of the view AND 100 -> BOTTOM of the view
     *  2) the initial value is equal to the height of view straight middle line
     *
     *  additional tips
     *  1) extraY value can be negative to produce up curve
     *  2) setting extraY value for both points to zero will produce straight line
     *  2) use same values for first and second control points to convert it into a single control point
     */

    private var firstControlPointX: Float = 0f
    private var firstControlPointExtraY: Float = 0f
    private var secondControlPointX: Float = 0f
    private var secondControlPointExtraY: Float = 0f


    /**
     * shadowRadius of customShadow (from app:curveShadowRadius attribute)
     * @see setupShadow
     */
    private var shadowRadius: Float = 0f

    /**
     * boolean indicating whether or not to draw custom shadow
     * @see setupShadow
     */
    private var drawCustomShadow = false
        set(value) {
            field = shadowRadius > 0f && value
        }

    /**
     * paint used to draw the path
     */
    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
    }

    /**
     * paint used to draw the shadow
     */
    private val shadowPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        isDither = true
    }

    /**
     * the extra height needed by the curve
     */
    private var extraCurveSpace = 0f


    /**
     * the points used to draw the curve
     */
    private lateinit var bezierPoints:List<PointF>


    /**
     * initializing values from attributes
     */
    init {
        context.withStyledAttributes(attrs, R.styleable.CurveView) {
            startColor = getColor(R.styleable.CurveView_startColor, Color.CYAN)
            endColor = getColor(R.styleable.CurveView_endColor, Color.YELLOW)

            startColorX = getFloat(R.styleable.CurveView_startColorX, 0.1f).coerceIn(0f, 1f)
            startColorY = getFloat(R.styleable.CurveView_startColorY, 0.0f).coerceIn(0f, 1f)
            endColorX = getFloat(R.styleable.CurveView_endColorX, 0.65f).coerceIn(0f, 1f)
            endColorY = getFloat(R.styleable.CurveView_endColorY, 1f).coerceIn(0f, 1f)


            firstControlPointX =
                getFloat(R.styleable.CurveView_firstControlPointX, 0.4f).coerceIn(0f, 1f)
            firstControlPointExtraY =
                getDimension(R.styleable.CurveView_firstControlPointExtraY, 120f)
            secondControlPointX =
                getFloat(R.styleable.CurveView_secondControlPointX, 0.5f).coerceIn(0f, 1f)
            secondControlPointExtraY =
                getDimension(R.styleable.CurveView_secondControlPointExtraY, -180f)

            shadowRadius = getDimension(R.styleable.CurveView_curveShadowRadius, 0f)
            shadowPaint.apply {
                color = getColor(R.styleable.CurveView_curveShadowColor, Color.BLACK)
                if (shadowRadius > 0f) maskFilter = BlurMaskFilter(shadowRadius, BlurMaskFilter.Blur.OUTER)
            }
        }

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        setBezierPoints()
        setExtraCurveSpace(bezierPoints)
        // indent curve height to provide extra space
        bezierPoints.forEach { it.y -= extraCurveSpace }
        paint.shader = getGradientColor()
        createPath()
        setupShadow()
    }

    /**
     * elevation shadow is drawn when:
     * elevation attr > 0 AND path.isConvex= true (the drawn shape is convex)
     *
     * custom shadow is drawn when:
     * - curveShadowRadius is > 0
     * AND
     * - android:clipChildren attr of parent layouts = false
     * AND
     * 1) elevation is 0 or less
     *       <<OR>>
     * 2) elevation is not working (when path.isConvex= false)
     */
    private fun setupShadow() {
        outlineProvider = if (path.isConvex) {
            drawCustomShadow = elevation <= 0f

            object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    //create a rectangular outline
                    outline.setConvexPath(path)
                }
            }

        } else {
            drawCustomShadow = true
            null
        }
    }


    private fun createPath() {
        val fWidth= width.toFloat()
        path.apply {
            reset()
            moveTo(fWidth, 0f)
            lineTo(0f, 0f)
            lineTo(bezierPoints[0].x, bezierPoints[0].y)
            cubicTo(
                bezierPoints[1].x, bezierPoints[1].y,
                bezierPoints[2].x, bezierPoints[2].y,
                bezierPoints[3].x, bezierPoints[3].y
            )
            lineTo(fWidth, 0f)
        }
    }

    private fun getGradientColor(): LinearGradient {
        val fHeight= height.toFloat()
        val fWidth= width.toFloat()

        return LinearGradient(
            fWidth * startColorX,
            fHeight * startColorY,
            fWidth * endColorX,
            (fHeight - extraCurveSpace) * endColorY,
            startColor, //FFA78B
            endColor, // DE9BE8
            Shader.TileMode.CLAMP
        )
    }

    private fun setExtraCurveSpace(
        bezierPoints: List<PointF>) {
        val highestPoint = highestPoint(bezierPoints)
        extraCurveSpace = highestPoint.y - height
    }

    private fun setBezierPoints(
    ) {
        val fHeight= height.toFloat()
        val fWidth= width.toFloat()

        bezierPoints=  listOf(
            PointF(0f, fHeight),
            PointF(fWidth * firstControlPointX, fHeight + firstControlPointExtraY),
            PointF(fWidth * secondControlPointX, fHeight + secondControlPointExtraY),
            PointF(fWidth, fHeight)
        )
    }


    private operator fun PointF.times(scale: Float) = PointF(x * scale, y * scale)
    private fun lerp(p0: PointF, p1: PointF, t: Float) = p0 * (1 - t) + p1 * t
    private fun highestPoint(bezierPoints: List<PointF>): PointF {
        val points = mutableListOf<PointF>()

        for (i in 0..10) {

            val t = i / 10f
            val res1 = lerp(bezierPoints[0], bezierPoints[1], t)
            val res2 = lerp(bezierPoints[1], bezierPoints[2], t)
            val res3 = lerp(bezierPoints[2], bezierPoints[3], t)

            val finalRes1 = lerp(res1, res2, t)
            val finalRes2 = lerp(res2, res3, t)

            points.add(lerp(finalRes1, finalRes2, t))
        }

        return points.maxBy { it.y }!!
    }


    override fun onDraw(canvas: Canvas) {
        if(drawCustomShadow) canvas.drawPath(path, shadowPaint)
        canvas.drawPath(path, paint)
        super.onDraw(canvas)
    }

}