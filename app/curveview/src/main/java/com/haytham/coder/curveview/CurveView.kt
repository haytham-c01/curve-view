package com.haytham.coder.curveview

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.plus


class CurveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object{
        const val TAG= "CurvedView"
    }

    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
    }
    private val path= Path()

    @ColorInt
    private var startColor:Int= 0
    @ColorInt
    private var endColor:Int= 0

    private var startColorX:Float=0f
    private var startColorY:Float=0f
    private var endColorX:Float=0f
    private var endColorY:Float=0f

    private var firstControlPointX:Float=0f
    private var firstControlPointExtraY:Float=0f
    private var secondControlPointX:Float=0f
    private var secondControlPointExtraY:Float=0f

    init {
        context.withStyledAttributes(attrs, R.styleable.CurveView) {
            startColor = getColor(R.styleable.CurveView_startColor,  Color.CYAN)
            endColor = getColor(R.styleable.CurveView_endColor, Color.YELLOW)

            startColorX= getFloat(R.styleable.CurveView_startColorX, 0.1f).coerceIn(0f, 1f)
            startColorY= getFloat(R.styleable.CurveView_startColorY, 0.0f).coerceIn(0f, 1f)
            endColorX= getFloat(R.styleable.CurveView_endColorX, 0.65f).coerceIn(0f, 1f)
            endColorY= getFloat(R.styleable.CurveView_endColorY, 1f).coerceIn(0f, 1f)


            firstControlPointX= getFloat(R.styleable.CurveView_firstControlPointX, 0.4f).coerceIn(0f, 1f)
            firstControlPointExtraY= getDimension(R.styleable.CurveView_firstControlPointExtraY, 120f)
            secondControlPointX= getFloat(R.styleable.CurveView_secondControlPointX, 0.5f).coerceIn(0f, 1f)
            secondControlPointExtraY= getDimension(R.styleable.CurveView_secondControlPointExtraY, -180f)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val fHeight= h.toFloat()
        val fWidth= w.toFloat()


        val bezierPoints= listOf(
            PointF(0f, fHeight ),
            PointF(fWidth * firstControlPointX, fHeight + firstControlPointExtraY ),
            PointF(fWidth * secondControlPointX, fHeight + secondControlPointExtraY ),
            PointF(fWidth, fHeight )
        )

        val highestPoint= highestPoint(bezierPoints)

        Log.d(TAG, bezierPoints.toString())
        Log.d(TAG, highestPoint.toString())
        val extraCurveSpace= highestPoint.y - fHeight

        bezierPoints.forEach { it.y -= extraCurveSpace }
        paint.shader = LinearGradient(
            fWidth * startColorX,
            fHeight * startColorY,
            fWidth * endColorX,
            (fHeight - extraCurveSpace) * endColorY,
            startColor, //FFA78B
            endColor, // DE9BE8
            Shader.TileMode.CLAMP)

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


        outlineProvider= object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                //create a rectangular outline
                outline.setConvexPath(path)
            }
        }


    }


    private operator fun PointF.times(scale:Float) = PointF( x * scale,  y * scale)
    private fun lerp(p0:PointF, p1:PointF, t:Float) =  p0 * (1-t) +  p1 * t
    private fun highestPoint(bezierPoints:List<PointF>): PointF{
        val points= mutableListOf<PointF>()

        for(i in 0..10) {

            val t = i / 10f
            val res1 = lerp(bezierPoints[0], bezierPoints[1], t)
            val res2 = lerp(bezierPoints[1], bezierPoints[2], t)
            val res3 = lerp(bezierPoints[2], bezierPoints[3], t)

            val finalRes1= lerp(res1, res2, t)
            val finalRes2= lerp(res2, res3, t)

            points.add(lerp(finalRes1, finalRes2, t))
        }

        return points.maxBy { it.y }!!
    }


    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(path, paint)
        super.onDraw(canvas)
    }

}