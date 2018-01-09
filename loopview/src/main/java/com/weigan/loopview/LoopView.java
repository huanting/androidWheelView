package com.weigan.loopview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Weidongjian on 2015/8/18.
 */
public class LoopView extends View {

    private float scaleX = 1.05F;

    private static final int DEFAULT_TEXT_SIZE = (int) (Resources.getSystem().getDisplayMetrics().density * 15);

    private static final float DEFAULT_LINE_SPACE = 2f;

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private static final int DEFAULT_VISIBIE_ITEMS = 9;

    public enum ACTION {
        CLICK, FLING, DAGGLE
    }

    private Context context;

    Handler handler;
    private GestureDetector flingGestureDetector;
    OnItemSelectedListener onItemSelectedListener;
    OnItemClickListener onItemClickListener;

    // Timer mTimer;
    ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> mFuture;

    private Paint paintTopText;
    private Paint paintBottomText;
    private Paint paintCenterText;
    private Paint paintIndicator;


    private TextShaderCallback topShaderCallback;
    private TextShaderCallback centerShaderCallback;
    private TextShaderCallback bottomShaderCallback;
    private TextShaderCallback indicatorShaderCallback;

//    List<ItemPara> items;
    private LoopAdapter mAdapter;

    private int textSize;
    protected int maxTextHeight;
    protected double halfCircumOffset; //在根据半周长计算item高度的时候，有可能不被正常，造成正常显示时，最后一个item显示一条缝

    private int topTextColor;
    private int centerTextColor;
    private int bottomTextColor;
    private int dividerColor;

    protected float lineSpacingMultiplier;
    protected boolean isLoop;

    private int firstLineY; //中心item项上面分割线
    private int secondLineY;

    protected int totalScrollY;
    protected int initPosition;
    protected int selectedItem;
    protected int preCurrentIndex;
    protected int change;

    private int itemsRealCount;

    private SparseArray<ItemPara> drawingItemParas = new SparseArray<>();

    private int measuredHeight;
    private int measuredWidth;

    private double halfCircumference;
    private int radius;

    private int mOffset = 0;
    private float previousY;
    private long startTime = 0;

    private Rect tempRect = new Rect();

    private int paddingLeft, paddingRight;

    /**
     * set text line space, must more than 1
     * @param lineSpacingMultiplier
     */
    public void setLineSpacingMultiplier(float lineSpacingMultiplier) {
        if (lineSpacingMultiplier > 1.0f) {
            this.lineSpacingMultiplier = lineSpacingMultiplier;
        }
    }

    /**
     * set outer text color
     * @param centerTextColor
     */
    public void setCenterTextColor(int centerTextColor) {
        paintCenterText.setColor(centerTextColor);
    }

    /**
     * set center text color
     * @param outerTextColor
     */
    public void setOuterTextColor(int outerTextColor) {
        paintTopText.setColor(outerTextColor);
        paintBottomText.setColor(outerTextColor);
    }

    /**
     * set center text color
     * @param topTextColor
     */
    public void setTopTextColor(int topTextColor) {
        paintTopText.setColor(topTextColor);
    }

    /**
     * set center text color
     * @param bottomTextColor
     */
    public void setBottomTextColor(int bottomTextColor) {
        paintBottomText.setColor(bottomTextColor);
    }

    /**
     * set divider color
     * @param dividerColor
     */
    public void setDividerColor(int dividerColor) {
        paintIndicator.setColor(dividerColor);
    }

    public void setTopShaderCallback(TextShaderCallback topShaderCallback) {
        this.topShaderCallback = topShaderCallback;
    }

    public void setCenterShaderCallback(TextShaderCallback centerShaderCallback) {
        this.centerShaderCallback = centerShaderCallback;
    }

    public void setBottomShaderCallback(TextShaderCallback bottomShaderCallback) {
        this.bottomShaderCallback = bottomShaderCallback;
    }

    public void setIndicatorShaderCallback(TextShaderCallback indicatorShaderCallback) {
        this.indicatorShaderCallback = indicatorShaderCallback;
    }

    public LoopView(Context context) {
        super(context);
        initLoopView(context, null);
    }

    public LoopView(Context context, AttributeSet attributeset) {
        super(context, attributeset);
        initLoopView(context, attributeset);
    }

    public LoopView(Context context, AttributeSet attributeset, int defStyleAttr) {
        super(context, attributeset, defStyleAttr);
        initLoopView(context, attributeset);
    }

    private void initLoopView(Context context, AttributeSet attributeset) {
        this.context = context;
        handler = new MessageHandler(this);
        flingGestureDetector = new GestureDetector(context, new LoopViewGestureListener(this));
        flingGestureDetector.setIsLongpressEnabled(false);

        TypedArray typedArray = context.obtainStyledAttributes(attributeset, R.styleable.androidWheelView);
        textSize = typedArray.getInteger(R.styleable.androidWheelView_awv_textsize, DEFAULT_TEXT_SIZE);
        textSize = (int) (Resources.getSystem().getDisplayMetrics().density * textSize);
        lineSpacingMultiplier = typedArray.getFloat(R.styleable.androidWheelView_awv_lineSpace, DEFAULT_LINE_SPACE);
        topTextColor = typedArray.getInteger(R.styleable.androidWheelView_awv_topTextColor, 0xffafafaf);
        centerTextColor = typedArray.getInteger(R.styleable.androidWheelView_awv_centerTextColor, 0xff313131);
        bottomTextColor = typedArray.getInteger(R.styleable.androidWheelView_awv_bottomTextColor, 0xffafafaf);
        dividerColor = typedArray.getInteger(R.styleable.androidWheelView_awv_dividerTextColor, 0xffc5c5c5);
        int visibleCount =
            typedArray.getInteger(R.styleable.androidWheelView_awv_itemsVisibleCount, 0);

        if(visibleCount != 0)
            itemsRealCount = visibleCount + 2;

        if (itemsRealCount % 2 == 0) {
            itemsRealCount = DEFAULT_VISIBIE_ITEMS;
        }
        isLoop = typedArray.getBoolean(R.styleable.androidWheelView_awv_isLoop, true);
        typedArray.recycle();

        totalScrollY = 0;
        initPosition = -1;

        initPaints();
    }

    /**
     * visible item count, must be odd number
     *
     * @param visibleNumber
     */
    public void setItemsVisibleCount(int visibleNumber) {
        if (visibleNumber % 2 == 0) {
            return;
        }
        //add the top item and bottom item
        visibleNumber = visibleNumber + 2;
        if (visibleNumber != itemsRealCount) {
            itemsRealCount = visibleNumber;
        }
    }

    private void initPaints() {
        paintTopText = new Paint();
        paintTopText.setColor(topTextColor);
        paintTopText.setAntiAlias(true);
        paintTopText.setTypeface(Typeface.MONOSPACE);
        paintTopText.setTextSize(textSize);

        paintCenterText = new Paint();
        paintCenterText.setColor(centerTextColor);
        paintCenterText.setAntiAlias(true);
        paintCenterText.setTextScaleX(scaleX);
        paintCenterText.setTypeface(Typeface.MONOSPACE);
        paintCenterText.setTextSize(textSize);

        paintBottomText = new Paint();
        paintBottomText.setColor(bottomTextColor);
        paintBottomText.setAntiAlias(true);
        paintBottomText.setTypeface(Typeface.MONOSPACE);
        paintBottomText.setTextSize(textSize);

        paintIndicator = new Paint();
        paintIndicator.setColor(dividerColor);
        paintIndicator.setAntiAlias(true);

    }

    private void remeasure() {
        if (mAdapter == null) {
            return;
        }

        measuredWidth = getMeasuredWidth();

        measuredHeight = getMeasuredHeight();

        if (measuredWidth == 0 || measuredHeight == 0) {
            return;
        }

        paddingLeft = getPaddingLeft();
        paddingRight = getPaddingRight();

        measuredWidth = measuredWidth - paddingRight;

        paintCenterText.getTextBounds("\u661F\u671F", 0, 2, tempRect); // 星期
        maxTextHeight = tempRect.height();
        halfCircumference = measuredHeight * Math.PI / 2;
        //从顶点开始画,所以需要-1
        float tempVal = lineSpacingMultiplier * (itemsRealCount - 1);
        maxTextHeight = (int) (halfCircumference / tempVal);
        halfCircumOffset = halfCircumference - maxTextHeight * tempVal;

        //半径
        radius = measuredHeight / 2;

        //中心item项上面和下面的分割线
        firstLineY = (int) ((measuredHeight - lineSpacingMultiplier * maxTextHeight) / 2.0F);
        secondLineY = (int) ((measuredHeight + lineSpacingMultiplier * maxTextHeight) / 2.0F);
        if (initPosition == -1) {
            if (isLoop) {
                initPosition = (mAdapter.getCount() + 1) / 2;
            } else {
                initPosition = 0;
            }
        }

        //更新paint
        if(topShaderCallback != null) {
            topShaderCallback.setShader(paintTopText, paddingLeft, getPaddingTop(),
                    paddingLeft + measuredWidth, getPaddingTop() + maxTextHeight);
        }

        if(centerShaderCallback != null) {
            centerShaderCallback.setShader(paintCenterText, paddingLeft, getPaddingTop(),
                    paddingLeft + measuredWidth, getPaddingTop() + maxTextHeight);
        }

        if(bottomShaderCallback != null) {
            bottomShaderCallback.setShader(paintBottomText, paddingLeft, getPaddingTop(),
                    paddingLeft + measuredWidth, getPaddingTop() + maxTextHeight);
        }

        if(indicatorShaderCallback != null) {
            indicatorShaderCallback.setShader(paintIndicator, paddingLeft, 0, paddingLeft + measuredWidth, 0);
        }

        preCurrentIndex = initPosition;
    }

    void smoothScroll(ACTION action) {
        cancelFuture();
        if (action == ACTION.FLING || action == ACTION.DAGGLE) {
            float itemHeight = lineSpacingMultiplier * maxTextHeight;
            mOffset = (int) ((totalScrollY % itemHeight + itemHeight) % itemHeight);
            if ((float) mOffset > itemHeight / 2.0F) {
                mOffset = (int) (itemHeight - (float) mOffset);
            } else {
                mOffset = -mOffset;
            }
        }
        mFuture =
            mExecutor.scheduleWithFixedDelay(new SmoothScrollTimerTask(this, mOffset), 0, 10, TimeUnit.MILLISECONDS);
    }

    protected final void scrollBy(float velocityY) {
        cancelFuture();
        // change this number, can change fling speed
        int velocityFling = 10;
        mFuture = mExecutor.scheduleWithFixedDelay(new InertiaTimerTask(this, velocityY), 0, velocityFling,
            TimeUnit.MILLISECONDS);
    }

    public void cancelFuture() {
        if (mFuture != null && !mFuture.isCancelled()) {
            mFuture.cancel(true);
            mFuture = null;
        }
    }

    /**
     * set not loop
     */
    public void setNotLoop() {
        isLoop = false;
    }

    /**
     * set text size in dp
     * @param size
     */
    public final void setTextSize(float size) {
        if (size > 0.0F) {
            textSize = (int) (context.getResources().getDisplayMetrics().density * size);
            paintTopText.setTextSize(textSize);
            paintCenterText.setTextSize(textSize);
            paintBottomText.setTextSize(textSize);
        }
    }

    public final void setInitPosition(int initPosition) {
        if (initPosition < 0) {
            this.initPosition = 0;
        } else {
            if (getCount() > initPosition) {
                this.initPosition = initPosition;
            }
        }
    }

    public final void setListener(OnItemSelectedListener OnItemSelectedListener) {
        onItemSelectedListener = OnItemSelectedListener;
    }

    public final void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public final void setAdapter(LoopAdapter adapter) {
        mAdapter = adapter;
        remeasure();
        invalidate();
    }

    public final int getSelectedItem() {
        return selectedItem;
    }
    //
    // protected final void scrollBy(float velocityY) {
    // Timer timer = new Timer();
    // mTimer = timer;
    // timer.schedule(new InertiaTimerTask(this, velocityY, timer), 0L, 20L);
    // }

    protected final void onItemSelected() {
        if (onItemSelectedListener != null) {
            postDelayed(new OnItemSelectedRunnable(this), 200L);
        }
    }

    /**
     * link https://github.com/weidongjian/androidWheelView/issues/10
     *
     * @param scaleX
     */
    public void setScaleX(float scaleX) {
        this.scaleX = scaleX;
    }

    /**
     * set current item position
     * @param position
     */
    public void setCurrentPosition(int position) {
        int size = getCount();
        if (size == 0) {
            return;
        }

        if (position >= 0 && position < size && position != selectedItem) {
            initPosition = position;
            totalScrollY = 0;
            mOffset = 0;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int itemCount = getCount();
        if (itemCount == 0) {
            return;
        }

        change = (int) (totalScrollY / (lineSpacingMultiplier * maxTextHeight));
        preCurrentIndex = initPosition + change % itemCount;

        if (!isLoop) {
            if (preCurrentIndex < 0) {
                preCurrentIndex = 0;
            }
            if (preCurrentIndex > itemCount - 1) {
                preCurrentIndex = itemCount - 1;
            }
        } else {
            if (preCurrentIndex < 0) {
                preCurrentIndex = itemCount + preCurrentIndex;
            }
            if (preCurrentIndex > itemCount - 1) {
                preCurrentIndex = preCurrentIndex - itemCount;
            }
        }

        int j2 = (int) (totalScrollY % (lineSpacingMultiplier * maxTextHeight));
        // put value to drawingString
        int k1 = 0;

        //计算当前要显示的string
        drawingItemParas.clear();

        while (k1 < itemsRealCount) {
            int l1 = preCurrentIndex - (itemsRealCount / 2 - k1);
            if (isLoop) {
                while (l1 < 0) {
                    l1 = l1 + itemCount;
                }
                while (l1 > itemCount - 1) {
                    l1 = l1 - itemCount;
                }
                drawingItemParas.put(k1, new ItemPara(l1));
            } else if (l1 < 0) {
                drawingItemParas.put(k1,new ItemPara());
            } else if (l1 > itemCount - 1) {
                drawingItemParas.put(k1,new ItemPara());
            } else {
               // drawingItemParas[k1] = items.get(l1);
                drawingItemParas.put(k1,new ItemPara(l1));
            }
            k1++;
        }
        //画分割线
        canvas.drawLine(paddingLeft, firstLineY, measuredWidth, firstLineY, paintIndicator);
        canvas.drawLine(paddingLeft, secondLineY, measuredWidth, secondLineY, paintIndicator);

        int i = 0;
        //修改文字居中问题
        float itemHeight = maxTextHeight * lineSpacingMultiplier;
        Paint.FontMetrics topFm = paintTopText.getFontMetrics();
        Paint.FontMetrics centerFm = paintCenterText.getFontMetrics();
        Paint.FontMetrics bottomFm = paintBottomText.getFontMetrics();

        float topTextH = topFm.descent - topFm.ascent;
        float topY = (maxTextHeight - topTextH)/2 - topFm.ascent;

        float bottomTextH = bottomFm.descent - bottomFm.ascent;
        float bottomY = (maxTextHeight - bottomTextH)/2 - bottomFm.ascent;

        float centerTextH = centerFm.descent - centerFm.ascent;
        float centerY = (maxTextHeight - centerTextH)/2 - centerFm.ascent;

        int previousY = -1;
        double correctError = 0;
        while (i < itemsRealCount) {
            canvas.save();

            //当前夹角弧度
            double radian = ((itemHeight * i - j2 + correctError) * Math.PI) / halfCircumference;
            if(correctError < halfCircumOffset) {
                if(correctError + DEFAULT_LINE_SPACE > halfCircumOffset)
                    correctError = halfCircumOffset;
                else
                    correctError += DEFAULT_LINE_SPACE;
            }
            if (radian >= Math.PI || radian <= 0) {
                canvas.restore();
            } else {
                //主要是为了画两种颜色
                ItemPara itemData = drawingItemParas.get(i);
                String desc = getDescription(itemData.index);
                if(TextUtils.isEmpty(desc)) {
                    i++;
                    continue;
                }

                //当前item中心点投影点到yTop的距离
                double cosVal = Math.cos(radian);
                double sinVal = Math.sin(radian);

                double offsetYToYVertex = radius - cosVal * radius;
                double prjItemH = sinVal * itemHeight;
                //去除间距后的移动位置
                int translateY = (int) (offsetYToYVertex - (sinVal * maxTextHeight) / 2D);
                int hasSpaceTranslateY = (int)(offsetYToYVertex - prjItemH / 2D);
                canvas.translate(0.0F, translateY);
                //对画布放大缩小实现投影显示
                canvas.scale(1.0F, (float) sinVal);

                if(itemData.index != -1) {
                    if(previousY == -1)
                        previousY = hasSpaceTranslateY;
                    itemData.displayRect = new Rect(paddingLeft, previousY, measuredWidth, (int) (hasSpaceTranslateY + prjItemH));
                    previousY = itemData.displayRect.bottom + 1;
                }

                if (translateY <= firstLineY && maxTextHeight + translateY >= firstLineY) {
                    // first divider
                    canvas.save();
                    canvas.clipRect(0, 0, measuredWidth, firstLineY - translateY);
                    canvas.drawText(desc, getTextX(desc, paintTopText, tempRect),
                        topY, paintTopText);
                    canvas.restore();
                    canvas.save();
                    canvas.clipRect(0, firstLineY - translateY, measuredWidth, (int) (itemHeight));
                    canvas.drawText(desc, getTextX(desc, paintCenterText, tempRect),
                        centerY, paintCenterText);
                    canvas.restore();
                } else if (translateY <= secondLineY && maxTextHeight + translateY >= secondLineY) {
                    // second divider
                    canvas.save();
                    canvas.clipRect(0, 0, measuredWidth, secondLineY - translateY);
                    canvas.drawText(desc, getTextX(desc, paintCenterText, tempRect),
                        centerY, paintCenterText);
                    canvas.restore();
                    canvas.save();
                    canvas.clipRect(0, secondLineY - translateY, measuredWidth, (int) (itemHeight));
                    canvas.drawText(desc, getTextX(desc, paintBottomText, tempRect),
                        bottomY, paintBottomText);
                    canvas.restore();
                } else if (translateY >= firstLineY && maxTextHeight + translateY <= secondLineY) {
                    // center item
                    canvas.clipRect(0, 0, measuredWidth, (int) (itemHeight));
                    canvas.drawText(desc, getTextX(desc, paintCenterText, tempRect),
                        centerY, paintCenterText);
                    selectedItem = itemData.index;
                } else {
                    // other item
                    canvas.clipRect(0, 0, measuredWidth, (int) (itemHeight));
                    if(translateY < firstLineY)
                        canvas.drawText(desc, getTextX(desc, paintTopText, tempRect),
                        topY, paintTopText);
                    else
                        canvas.drawText(desc, getTextX(desc, paintBottomText, tempRect), bottomY, paintBottomText);
                }
                canvas.restore();
            }
            i++;
        }
    }

    // text start drawing position
    private int getTextX(String a, Paint paint, Rect rect) {
        paint.getTextBounds(a, 0, a.length(), rect);
        int textWidth = rect.width();
        textWidth *= scaleX;
        return (measuredWidth - paddingLeft - textWidth) / 2 + paddingLeft;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        remeasure();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean eventConsumed = flingGestureDetector.onTouchEvent(event);
        float itemHeight = lineSpacingMultiplier * maxTextHeight;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startTime = System.currentTimeMillis();
                cancelFuture();
                previousY = event.getRawY();
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;

            case MotionEvent.ACTION_MOVE:
//                float dy = previousY - event.getRawY();
//                previousY = event.getRawY();
//
//                totalScrollY = (int) (totalScrollY + dy);
//
//                if (!isLoop) {
//                    float top = -initPosition * itemHeight;
//                    float bottom = (items.size() - 1 - initPosition) * itemHeight;
//
//                    if (totalScrollY < top) {
//                        totalScrollY = (int) top;
//                    } else if (totalScrollY > bottom) {
//                        totalScrollY = (int) bottom;
//                    }
//                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            default:
                //Fix the bug: when click the blank area(top or bottom, noloop mode)  quickly, the item will move.
                if (!eventConsumed) {
                    float y = event.getY();
                    //yTop到当前touchUp点的弧长
                    double l = Math.acos((radius - y) / radius) * radius;
                    //计算yTop点到touch up点显示item数量
                    int circlePosition = (int) ((l + itemHeight / 2) / itemHeight);

                    float extraOffset = (totalScrollY % itemHeight + itemHeight) % itemHeight;
                    mOffset = (int) ((circlePosition - itemsRealCount / 2) * itemHeight - extraOffset);

                    //when long press the item, the clicked item can't move, but it do in ios system.
                    if ( getClickItemIndex(event) == -1) {
                        smoothScroll(ACTION.DAGGLE);
                    } else {
                        smoothScroll(ACTION.CLICK);
                    }
            }
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
        }

        invalidate();
        return true;
    }

    //get the index of the item that is clicked on.
    private int getClickItemIndex(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int clickIndex = -1;
        for(int i=0; i< drawingItemParas.size(); i++) {
            ItemPara indexString = drawingItemParas.valueAt(i);
            Rect rect = indexString.displayRect;
            if(rect != null && x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom) {
                clickIndex = indexString.index;
                break;
            }
        }
        return clickIndex;
    }

    //add click listener
    protected boolean onItemClick(MotionEvent event) {
        int clickIndex = getClickItemIndex(event);

        if(clickIndex != -1 && onItemClickListener != null) {
            onItemClickListener.onItemClick(clickIndex);
        }
        return clickIndex != -1;
    }

    /**
     * original code: drag ui event the moved distance is very samll.
     * I use the GestureDetector to verify whether it is moving.
     */
    protected boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        boolean result = true;
        totalScrollY = (int) (totalScrollY + distanceY);
        float itemHeight = maxTextHeight * lineSpacingMultiplier;

        if (!isLoop) {
            if(getCount() > 0) {
                float top = -initPosition * itemHeight;
                float bottom = (getCount() - 1 - initPosition) * itemHeight;

                if (totalScrollY < top) {
                    totalScrollY = (int) top;
                } else if (totalScrollY > bottom) {
                    totalScrollY = (int) bottom;
                }
            }
        }
        return result;
    }

    protected int getCount() {
        if(mAdapter != null)
            return mAdapter.getCount();
        return 0;
    }

    private String getDescription(int index) {
        if(mAdapter != null && mAdapter.getCount() > 0 && index >= 0 && index < mAdapter.getCount())
            return mAdapter.getDescription(index);
        return null;
    }

    class  ItemPara {
        public  ItemPara(){
            this(-1);
        }

        public  ItemPara(int index){
            this.index = index;
        }

        private Rect displayRect; //显示区域
        private int index;
    }

    public interface TextShaderCallback {
        void setShader(Paint paint, int x0, int y0, int x1, int y1);
    }
}
