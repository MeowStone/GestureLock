package com.stone.gesturelock;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stone on 2017/7/19.
 *
 *  整体包含n*n个GestureLockView,每个GestureLockView间间隔mMarginBetweenLockView，
 *  最外层的GestureLockView与容器存在mMarginBetweenLockView的外边距
 *
 *  关于GestureLockView的边长（n*n）： n * mGestureLockViewWidth + ( n + 1 ) *
 *  mMarginBetweenLockView = mWidth ; 得：mGestureLockViewWidth = 4 * mWidth / ( 5
 *  mCount + 1 ) 注：mMarginBetweenLockView = mGestureLockViewWidth * 0.25 ;

 */

public class GestureLockViewGroup extends RelativeLayout {

    private static final String TAG = "GestureLockViewGroup";

    /**
     * 保存所有的GestureLockView
     */
    private GestureLockView[] mGestureLockViews;
    /**
     * 每个边上的GestureLockView的个数
     */
    private int mCount = 3;
    /**
     * 存储答案
     */
    private Integer [] mAnswer = {};



    private List<Integer> mFirstAnswer;

    private boolean isFirstTime = true;

    private boolean isActionDone = false;
    /**
     * 保存用户选中的GestureLockView的id
     */
    private List<Integer> mChoose = new ArrayList<>();

    private Paint mPaint;
    /**
     * 每个GestureLockView中间的间距 设置为：mGestureLockViewWidth * 25%
     */
    private int mMarginBetweenLockView = 30;
    /**
     * GestureLockView的边长 4 * mWidth / ( 5 * mCount + 1 )
     */
    private int mGestureLockViewWidth;

    /**
     * GestureLockView无手指触摸的状态下内圆的颜色
     */
    private int mNoFingerInnerCircleColor = getResources().getColor(R.color.inner_circle_normal_color);
    /**
     * GestureLockView无手指触摸的状态下外圆的颜色
     */
    private int mNoFingerOuterCircleColor = getResources().getColor(R.color.outer_circle_normal_color);
    /**
     * GestureLockView手指触摸的状态下内圆和外圆的颜色
     */
    private int mFingerOnColor = getResources().getColor(R.color.outer_inner_on_color);
    /**
     * GestureLockView手指抬起的状态下内圆和外圆的颜色
     */
    private int mFingerUpFailedColor = getResources().getColor(R.color.outer_inner_off_failed_color);

    private int mFingerUpDoneColor = getResources().getColor(R.color.outer_inner_off_done_color);

    /**
     * 宽度
     */
    private int mWidth;
    /**
     * 高度
     */
    private int mHeight;

    private Path mPath;
    /**
     * 指引线的开始位置x
     */
    private int mLastPathX;
    /**
     * 指引线的开始位置y
     */
    private int mLastPathY;
    /**
     * 指引下的结束位置
     */
    private Point mTmpTarget = new Point();

    /**
     * 最大尝试次数
     */
    private int mTryTimes = 3;
    /**
     * 回调接口
     */
    private OnGestureLockViewListener mOnGestureLockViewListener;

    public static final int ACTION_UNDEFINED = 0x000;
    public static final int ACTION_LOCK = 0x100;
    public static final int ACTION_UNLOCK = 0x200;
    public static final int ACTION_MODIFY = 0x300;
    private boolean isUnlocked = false;

    @IntDef({ACTION_UNDEFINED, ACTION_LOCK, ACTION_UNLOCK, ACTION_MODIFY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface GestureLockAction {}

    private @GestureLockAction int mAction = ACTION_UNDEFINED;

    public GestureLockViewGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureLockViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        /**
         * 获得所有自定义的参数的值
         */
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.GestureLockViewGroup, defStyleAttr, 0);
        int n = a.getIndexCount();
        for (int i = 0; i < n; i ++) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.GestureLockViewGroup_color_no_finger_inner_circle) {
                mNoFingerInnerCircleColor = a.getColor(attr,
                        mNoFingerInnerCircleColor);

            } else if (attr == R.styleable.GestureLockViewGroup_color_no_finger_outer_circle) {
                mNoFingerOuterCircleColor = a.getColor(attr,
                        mNoFingerOuterCircleColor);

            } else if (attr == R.styleable.GestureLockViewGroup_color_finger_on) {
                mFingerOnColor = a.getColor(attr, mFingerOnColor);

            } else if (attr == R.styleable.GestureLockViewGroup_color_finger_up) {
                mFingerUpFailedColor = a.getColor(attr, mFingerUpFailedColor);

            } else if (attr == R.styleable.GestureLockViewGroup_count) {
                mCount = a.getInt(attr, 3);

            } else if (attr == R.styleable.GestureLockViewGroup_tryTimes) {
                mTryTimes = a.getInt(attr, 5);
            }
        }
        a.recycle();

        // 初始化画笔
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        // mPaint.setStrokeWidth(20);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        // mPaint.setColor(Color.parseColor("#aaffffff"));
        mPath = new Path();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);

        mHeight = mWidth = mWidth < mHeight ? mWidth : mHeight;

        // setMeasuredDimension(mWidth, mHeight);

        // 初始化mGestureLockViews
        if (mGestureLockViews == null) {

            mGestureLockViews = new GestureLockView[mCount * mCount];
            // 计算每个GestureLockView的宽度
            mGestureLockViewWidth = (int) (4 * mWidth * 1.0f / (5 * mCount + 1));
            //计算每个GestureLockView的间距
            mMarginBetweenLockView = (int) (mGestureLockViewWidth * 0.25);
            // 设置画笔的宽度为GestureLockView的内圆直径稍微小点（不喜欢的话，随便设）
            mPaint.setStrokeWidth(mGestureLockViewWidth * 0.29f);

            for (int i = 0; i < mGestureLockViews.length; i++) {
                //初始化每个GestureLockView
                mGestureLockViews[i] = new GestureLockView(getContext(),
                        mNoFingerInnerCircleColor, mNoFingerOuterCircleColor,
                        mFingerOnColor, mFingerUpFailedColor, mFingerUpDoneColor);
                mGestureLockViews[i].setId(i + 1);
                //设置参数，主要是定位GestureLockView间的位置
                RelativeLayout.LayoutParams lockerParams = new RelativeLayout.LayoutParams(
                        mGestureLockViewWidth, mGestureLockViewWidth);

                // 不是每行的第一个，则设置位置为前一个的右边
                if (i % mCount != 0) {
                    lockerParams.addRule(RelativeLayout.RIGHT_OF,
                            mGestureLockViews[i - 1].getId());
                }
                // 从第二行开始，设置为上一行同一位置View的下面
                if (i > mCount - 1) {
                    lockerParams.addRule(RelativeLayout.BELOW,
                            mGestureLockViews[i - mCount].getId());
                }
                //设置右下左上的边距
                int rightMargin = mMarginBetweenLockView;
                int bottomMargin = mMarginBetweenLockView;
                int leftMagin = 0;
                int topMargin = 0;
                /**
                 * 每个View都有右外边距和底外边距 第一行的有上外边距 第一列的有左外边距
                 */
                if (i >= 0 && i < mCount)// 第一行
                {
                    topMargin = mMarginBetweenLockView;
                }
                if (i % mCount == 0)// 第一列
                {
                    leftMagin = mMarginBetweenLockView;
                }

                lockerParams.setMargins(leftMagin, topMargin, rightMargin,
                        bottomMargin);
                mGestureLockViews[i].setStatus(GestureLockView.STATUS_NO_FINGER);
                addView(mGestureLockViews[i], lockerParams);
            }

            Log.e(TAG, "mWidth = " + mWidth + " ,  mGestureViewWidth = "
                    + mGestureLockViewWidth + " , mMarginBetweenLockView = "
                    + mMarginBetweenLockView);

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(isActionDone || mTryTimes == 0 || mAction == ACTION_UNDEFINED) {
            return true;
        }
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
                // 重置
                break;
            case MotionEvent.ACTION_MOVE:
                mPaint.setColor(mFingerOnColor);
                mPaint.setAlpha(50);
                GestureLockView child = getChildIdByPos(x, y);
                if (child != null) {
                    int cId = child.getId();
                    if (!mChoose.contains(cId)) {
                        mChoose.add(cId);
                        child.setStatus(GestureLockView.STATUS_FINGER_ON);
                        // 设置指引线的起点
                        mLastPathX = child.getLeft() / 2 + child.getRight() / 2;
                        mLastPathY = child.getTop() / 2 + child.getBottom() / 2;

                        if (mChoose.size() == 1) {// 当前添加为第一个
                            mPath.moveTo(mLastPathX, mLastPathY);
                        } else {// 非第一个，将两者使用线连上
                            mPath.lineTo(mLastPathX, mLastPathY);
                        }
                    }
                }
                // 指引线的终点
                mTmpTarget.x = x;
                mTmpTarget.y = y;
                break;
            case MotionEvent.ACTION_UP:
                mPaint.setAlpha(50);
                // 回调是否成功
                if (mOnGestureLockViewListener != null && mChoose.size() > 0) {
                    switch (mAction) {
                        case ACTION_LOCK :
                            if(isFirstTime) {
                                isFirstTime = false;
                                changeItemStatus(true);
                                mPaint.setColor(mFingerUpDoneColor);
//                                mFirstAnswer = mChoose.subList(0, mChoose.size());
                                mFirstAnswer = new ArrayList<>(mChoose);
                                mOnGestureLockViewListener.onFirstLock(mAction, mFirstAnswer);
                                mHandler.sendEmptyMessageDelayed(100, 1000L);
                            } else {
                                if(checkLockAnswer()) {
                                    isActionDone = true;
                                    mPaint.setColor(mFingerUpDoneColor);
                                    mOnGestureLockViewListener.onSecondLockSucceeded(mAction, mFirstAnswer);
                                    mHandler.sendEmptyMessageDelayed(100, 1000L);
                                } else {
                                    isActionDone = false;
                                    mPaint.setColor(mFingerUpFailedColor);
                                    mOnGestureLockViewListener.onSecondLockFailed(mAction);
                                    mHandler.sendEmptyMessageDelayed(100, 500L);
                                }
                                changeItemStatus(isActionDone);
                            }
                            break;
                        case ACTION_UNLOCK :
                            //延迟重置
                            if(checkUnlockAnswer()) {
                                isActionDone = true;
                                mPaint.setColor(mFingerUpDoneColor);
                                mOnGestureLockViewListener.onUnlockCorrect(mAction, mChoose);
                                mHandler.sendEmptyMessageDelayed(100, 1000L);
                            } else {
                                isActionDone = false;
                                mPaint.setColor(mFingerUpFailedColor);
                                mOnGestureLockViewListener.onUnlockError(mAction, mChoose, mTryTimes);
                                mHandler.sendEmptyMessageDelayed(100, 500L);
                            }
                            changeItemStatus(isActionDone);
                            this.mTryTimes--;
                            if (this.mTryTimes == 0) {
                                mOnGestureLockViewListener.noMoreTry(mAction);
                            }
                            break;
                        case ACTION_MODIFY :
                            if(!isUnlocked) {
                                if(checkUnlockAnswer()) {
                                    isUnlocked = true;
                                    changeItemStatus(true);
                                    mPaint.setColor(mFingerUpDoneColor);
                                    mOnGestureLockViewListener.onUnlockCorrect(mAction, mChoose);
                                    mHandler.sendEmptyMessageDelayed(100, 1000L);
                                } else {
                                    changeItemStatus(false);
                                    mPaint.setColor(mFingerUpFailedColor);
                                    mOnGestureLockViewListener.onUnlockError(mAction, mChoose, mTryTimes);
                                    mHandler.sendEmptyMessageDelayed(100, 500L);
                                }
                                this.mTryTimes--;
                                if (this.mTryTimes == 0) {
                                    mOnGestureLockViewListener.noMoreTry(mAction);
                                }
                            } else {
                                if(isFirstTime) {
                                    isFirstTime = false;
                                    changeItemStatus(true);
                                    mPaint.setColor(mFingerUpDoneColor);
                                    mFirstAnswer = new ArrayList<>(mChoose);
                                    mOnGestureLockViewListener.onFirstLock(mAction, mFirstAnswer);
                                    mHandler.sendEmptyMessageDelayed(100, 1000L);
                                } else {
                                    if(checkLockAnswer()) {
                                        isActionDone = true;
                                        mPaint.setColor(mFingerUpDoneColor);
                                        mOnGestureLockViewListener.onSecondLockSucceeded(mAction, mFirstAnswer);
                                        mHandler.sendEmptyMessageDelayed(100, 1000L);
                                    } else {
                                        isActionDone = false;
                                        mPaint.setColor(mFingerUpFailedColor);
                                        mOnGestureLockViewListener.onSecondLockFailed(mAction);
                                        mHandler.sendEmptyMessageDelayed(100, 500L);
                                    }
                                    changeItemStatus(isActionDone);
                                }
                            }

                            break;
                        case ACTION_UNDEFINED :
                        default:
                            break;

                    }
                }

                Log.e(TAG, "mUnMatchExceedBoundary = " + mTryTimes);
                Log.e(TAG, "mChoose = " + mChoose);
                // 将终点设置位置为起点，即取消指引线
                mTmpTarget.x = mLastPathX;
                mTmpTarget.y = mLastPathY;

                // 计算每个元素中箭头需要旋转的角度
                for (int i = 0; i + 1 < mChoose.size(); i++) {
                    int childId = mChoose.get(i);
                    int nextChildId = mChoose.get(i + 1);

                    GestureLockView startChild = (GestureLockView) findViewById(childId);
                    GestureLockView nextChild = (GestureLockView) findViewById(nextChildId);

                    int dx = nextChild.getLeft() - startChild.getLeft();
                    int dy = nextChild.getTop() - startChild.getTop();
                    // 计算角度
                    int angle = (int) Math.toDegrees(Math.atan2(dy, dx)) + 90;
                    startChild.setArrowDegree(angle);
                }
                break;

        }
        invalidate();
        return true;
    }

    private void changeItemStatus(boolean isActionDone)
    {
        int status = isActionDone ? GestureLockView.STATUS_FINGER_UP_DONE : GestureLockView.STATUS_FINGER_UP_FAILED;
        for (GestureLockView gestureLockView : mGestureLockViews)
        {
            if (mChoose.contains(gestureLockView.getId()))
            {
                gestureLockView.setStatus(status);
            }
        }
    }

    /**
     *
     * 做一些必要的重置
     */
    private void reset() {
        mChoose.clear();
        mPath.reset();
        for (GestureLockView gestureLockView : mGestureLockViews)
        {
            gestureLockView.setStatus(GestureLockView.STATUS_NO_FINGER);
            gestureLockView.setArrowDegree(-1);
        }
    }

    /**
     * 检查用户绘制的手势是否正确
     * @return
     */
    private boolean checkUnlockAnswer() {
        if (mAnswer.length != mChoose.size())
            return false;

        for (int i = 0; i < mAnswer.length; i++) {
            if (mAnswer[i] != mChoose.get(i))
                return false;
        }
        return true;
    }

    private boolean checkLockAnswer() {
        return mFirstAnswer.size() == mChoose.size() && mFirstAnswer.equals(mChoose);
    }

    /**
     * 检查当前坐标是否在某个GestureLockView中
     * @param gestureLockView
     * @param x
     * @param y
     * @return
     */
    private boolean checkPositionInChild(View gestureLockView, int x, int y) {

        //设置了内边距，即x,y必须落入下GestureLockView的内部中间的小区域中，可以通过调整padding使得x,y落入范围不变大，或者不设置padding
        int padding = (int) (mGestureLockViewWidth * 0.15);

        return x >= gestureLockView.getLeft() + padding && x <= gestureLockView.getRight() - padding
                && y >= gestureLockView.getTop() + padding
                && y <= gestureLockView.getBottom() - padding;
    }

    /**
     * 通过x,y获得落入的GestureLockView
     * @param x
     * @param y
     * @return
     */
    private GestureLockView getChildIdByPos(int x, int y) {
        for (GestureLockView gestureLockView : mGestureLockViews) {
            if (checkPositionInChild(gestureLockView, x, y)) {
                return gestureLockView;
            }
        }
        return null;
    }

    public void setAction(@GestureLockAction int action) {
        this.mAction = action;
    }

    /**
     * 对外公布设置答案的方法
     *
     * @param answer
     */
    public void setAnswer(Integer [] answer) {
        if(answer == null || answer.length ==0) {
            return;
        }
        this.mAnswer = answer ;
    }

    /**
     * 设置最大实验次数
     *
     * @param boundary
     */
    public void setUnMatchExceedBoundary(int boundary) {
        this.mTryTimes = boundary;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        //绘制GestureLockView间的连线
        if (mPath != null) {
            canvas.drawPath(mPath, mPaint);
        }
        //绘制指引线
        if (mChoose.size() > 0) {
            if (mLastPathX != 0 && mLastPathY != 0)
                canvas.drawLine(mLastPathX, mLastPathY, mTmpTarget.x,
                        mTmpTarget.y, mPaint);
        }
    }

    private DelayHandler mHandler = new DelayHandler();

    private class DelayHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(mTryTimes == 0 || isActionDone) {
                return;
            }
            reset();
            postInvalidate();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //销毁一些对象回收资源
        if(mHandler != null) {
            mHandler.removeMessages(100);
            mHandler = null;
        }
    }

    /**
     * 设置回调接口
     *
     * @param listener
     */
    public void setOnGestureLockViewListener(OnGestureLockViewListener listener)
    {
        this.mOnGestureLockViewListener = listener;
    }

    public interface OnGestureLockViewListener {

        void onFirstLock(@GestureLockAction int action,List<Integer> answer);

        void onSecondLockSucceeded(@GestureLockAction int action,List<Integer> answer);

        void onSecondLockFailed(@GestureLockAction int action);
        /**
         *  手势解锁成功
         */
        void onUnlockCorrect(@GestureLockAction int action, List<Integer> answer);

        /**
         *  手势解锁失败
         *  @param answer
         *  @param chances
         */
        void onUnlockError(@GestureLockAction int action, List<Integer> answer, int chances);

        /**
         *  手势解锁超过尝试次数
         *  用于 解锁 和 更换密码
         *  @param action
         */
        void noMoreTry(@GestureLockAction int action);
    }
}
