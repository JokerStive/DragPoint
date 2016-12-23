package passionlife.skylife.com.testrxjava.Custom;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import passionlife.skylife.com.testrxjava.utils.GeometryUtil;

/**
 * Created by youke on 2016/12/21.
 * 贝塞尔曲线QQ小红点效果
 */
public class DragPoint extends View {

    private Paint mPaint;
    private int mCenterX, mCenterY;
    private float mDragRadius = 30;                  // 可拖拽圆的半径
    private float mInitFixedRadio = 30;                  // 可拖拽圆的半径
    private float mFixedRadius = mInitFixedRadio;                  // 固定圆的半径
    private float mDedDistance = 400;                  // 默认的范围
    private PointF mFixedCenter;                  // 固定圆的圆心
    private PointF mDragCenter;                  // 可拖拽圆的圆心
    private PointF mControlPoint = new PointF();                  // 控制点
    private boolean hasOverMoveDistance;                 // 是否又一次移动超过了范围
    private boolean isUpBackDistance;

    Path path = new Path();

    private PointF[] fixedTangenPoints;
    private PointF[] dragTangenPoints;

    public DragPoint(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DragPoint(Context context) {
        super(context);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mCenterX = getWidth() / 2;
        mCenterY = getHeight() / 2;

        mFixedCenter = new PointF(mCenterX, mCenterY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();

                //如果有有一次移动超过了默认范围，以后的移动都不在绘制固定圆和贝塞尔区域
                if (!hasOverMoveDistance) {
                    hasOverMoveDistance = hasOverMoveDistance(x, y);
                }
                updataDragCenter(x, y);
                break;

            case MotionEvent.ACTION_UP:
                isUpBackDistance = upBackDistance(event.getX(), event.getY());
                if (hasOverMoveDistance) {
                    if (isUpBackDistance) {
                        invalidate();
//                        reset();
                    } else {
                        setVisibility(GONE);
                    }
                } else {
                    inUp();
                }
                break;
        }

        return true;
    }

    private void reset() {
        hasOverMoveDistance = false;
        isUpBackDistance = false;
    }


    /**
     * 移动的点是否超过了默认可移动的范围
     */
    private boolean hasOverMoveDistance(float x, float y) {
        return GeometryUtil.getDistanceBetween2Points(new PointF(x, y), mFixedCenter) > mDedDistance;
    }


    /**
     * 抬起的时候是否回到了范围内
     */
    private boolean upBackDistance(float x, float y) {
        return GeometryUtil.getDistanceBetween2Points(new PointF(x, y), mFixedCenter) < mInitFixedRadio;
    }

    private void updataDragCenter(float x, float y) {
        if (mDragCenter == null) {
            mDragCenter = new PointF();
        }
        mDragCenter.set(x, y);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!hasOverMoveDistance) {
            //画固定的圆，半径随着两圆距离的增大而增大
            if (mDragCenter != null) {
                mFixedRadius = (float) (mInitFixedRadio - (GeometryUtil.getDistanceBetween2Points(mDragCenter, mFixedCenter)) * 0.05);
                Log.d("yk", "固定圆的半径 mFixedRadius = " + mFixedRadius);
            }
            canvas.drawCircle(mFixedCenter.x, mFixedCenter.y, mFixedRadius, mPaint);
            if (mDragCenter == null) {
                return;
            }

            //设置控制点
            mControlPoint.set((mFixedCenter.x + mDragCenter.x) / 2, (mFixedCenter.y + mDragCenter.y) / 2);


            //获取2个圆的4个外切点
            float dy = mDragCenter.y - mFixedCenter.y;
            float dx = mDragCenter.x - mFixedCenter.x;

            float k;
            if (dx != 0) {
                float k1 = dy / dx;
                k = -1 / k1;
            } else {
                k = 0;
            }
            fixedTangenPoints = GeometryUtil.getIntersectionPoints(mFixedCenter, mFixedRadius, (double) k);
            dragTangenPoints = GeometryUtil.getIntersectionPoints(mDragCenter, mDragRadius, (double) k);


            //画2条贝塞尔曲线跟2条直线形成的封闭图形
            path.reset();
            path.moveTo(fixedTangenPoints[0].x, fixedTangenPoints[0].y);
            path.quadTo(mControlPoint.x, mControlPoint.y, dragTangenPoints[0].x, dragTangenPoints[0].y);
            path.lineTo(dragTangenPoints[1].x, dragTangenPoints[1].y);
            path.quadTo(mControlPoint.x, mControlPoint.y, fixedTangenPoints[1].x, fixedTangenPoints[1].y);
            path.close();

            canvas.drawPath(path, mPaint);

            canvas.drawCircle(mDragCenter.x, mDragCenter.y, mDragRadius, mPaint);
        } else if (isUpBackDistance) {
            //如果有一次超出了移动范围，但是抬起的时候回到了默认的范围，就只画固定的圆
            canvas.drawCircle(mFixedCenter.x, mFixedCenter.y, mInitFixedRadio, mPaint);
            reset();
        } else {
            ///如果有一次超出了移动范围，只画可拖拽的圆
            canvas.drawCircle(mDragCenter.x, mDragCenter.y, mDragRadius, mPaint);
        }


    }

    /**
     * 放手回弹效果
     */
    private void inUp() {
        ValueAnimator animator = ValueAnimator.ofFloat(1.0f);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            boolean isStop=false;
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float fraction = animation.getAnimatedFraction();
                PointF byPercent = GeometryUtil.getPointByPercent(mDragCenter, mFixedCenter, fraction);
                if (byPercent.x==mFixedCenter.x && byPercent.y==mFixedCenter.y){
                    isStop = true;
                }
                if (!isStop){
                    updataDragCenter(byPercent.x, byPercent.y);
                }
            }
        });
        animator.setInterpolator(new OvershootInterpolator(4.0f));
        animator.setDuration(500);
        animator.start();
    }


}
