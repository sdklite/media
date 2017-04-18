package com.sdklite.media.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.sdklite.media.R;
import com.sdklite.gesture.DragGestureDetector;
import com.sdklite.gesture.RotateGestureDetector;
import com.sdklite.gesture.ScaleGestureDetector;

/**
 * Canvas view for image presenting
 */
public class CanvasView extends ImageView {

    private static final boolean DEFAULT_GESTURE_ENABLED = false;
    private static final boolean DEFAULT_GUIDELINE_VISIBILITY = false;
    private static final int DEFAULT_GUIDELINE_CORNER_LENGTH = 60;
    private static final int DEFAULT_GUIDELINE_CORNER_THICKNESS = 8;
    private static final int DEFAULT_GUIDELINE_BORDER_THICKNESS = 2;
    private static final int DEFAULT_GUIDELINE_COLUMNS = 3;
    private static final int DEFAULT_GUIDELINE_ROWS = 3;
    private static final int DEFAULT_GUIDELINE_THICKNESS = 1;
    private static final float DEFAULT_SCALE_RATE = 1.25f;
    private static final float DEFAULT_MAX_SCALE = 4f;

    private final Paint mCornerPaint;
    private final Paint mBorderPaint;
    private final Paint mGuidelinePaint;

    private final float[] mValues = new float[9];

    protected final Matrix mBaseMatrix = new Matrix();
    protected final Matrix mSupplementaryMatrix = new Matrix();
    protected final Matrix mDisplayMatrix = new Matrix();
    protected final RotateBitmap mBitmapDisplayed = new RotateBitmap(null);

    protected final DragGestureDetector mDragGestureDetector;
    protected final ScaleGestureDetector mScaleGestureDetector;
    protected final RotateGestureDetector mRotateGestureDetector;
    protected final GestureListener mGestureListener;

    private boolean mGestureEnabled;
    private int mGuidelineColumns;
    private int mGuidelineRows;
    private boolean mGuidelineVisibility;
    private float mGuidelineBorderThickness;
    private float mGuidelineThickness;
    private float mGuidelineCornerThickness;
    private float mGuidelineCornerLength;
    private float mScaleRate;
    private float mMaxScale;

    private int mThisWidth;
    private int mThisHeight;
    private Recycler mRecycler;
    private Runnable mOnLayoutRunnable;

    /**
     * Simple constructor to use when creating a view from code.
     *
     * @param context
     *            The Context the view is running in, through which it can access the current theme, resources, etc.
     */
    public CanvasView(final Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating a view from XML.
     *
     * @param context
     *            The Context the view is running in, through which it can access the current theme, resources, etc.
     * @param attrs
     *            The attributes of the XML tag that is inflating the view.
     */
    public CanvasView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute.
     * This constructor of View allows subclasses to use their own base style when they are inflating.
     *
     * @param context
     *            The Context the view is running in, through which it can access the current theme, resources, etc.
     * @param attrs
     *            The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr
     *            An attribute in the current theme that contains a reference to a style resource that supplies default values for the view. Can be 0 to not look for defaults.
     */
    public CanvasView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // gesture detection
        this.mGestureListener = new GestureListener();
        this.mDragGestureDetector = new DragGestureDetector(context, this.mGestureListener);
        this.mScaleGestureDetector = new ScaleGestureDetector(context, this.mGestureListener);
        this.mRotateGestureDetector = new RotateGestureDetector(context, this.mGestureListener);
        // corner paint
        this.mCornerPaint = new Paint();
        this.mCornerPaint.setStyle(Paint.Style.FILL);
        // border paint
        this.mBorderPaint = new Paint();
        this.mBorderPaint.setStyle(Paint.Style.STROKE);
        // guideline paint
        this.mGuidelinePaint = new Paint();
        this.mGuidelinePaint.setStyle(Paint.Style.STROKE);

        if (null != attrs) {
            final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CanvasView, 0, 0);

            this.mGestureEnabled = ta.getBoolean(R.styleable.CanvasView_canvasViewGestureEnabled, DEFAULT_GESTURE_ENABLED);
            this.mGuidelineVisibility = ta.getBoolean(R.styleable.CanvasView_canvasViewGuidelineVisible, DEFAULT_GUIDELINE_VISIBILITY);
            this.mGuidelineBorderThickness = ta.getDimension(R.styleable.CanvasView_canvasViewGuidelineBorderThickness, DEFAULT_GUIDELINE_BORDER_THICKNESS);
            this.mGuidelineColumns= Math.max(1, ta.getInteger(R.styleable.CanvasView_canvasViewGuidelineColumns, DEFAULT_GUIDELINE_COLUMNS));
            this.mGuidelineRows = Math.max(1, ta.getInteger(R.styleable.CanvasView_canvasViewGuidelineRows, DEFAULT_GUIDELINE_ROWS));
            this.mGuidelineThickness = ta.getDimension(R.styleable.CanvasView_canvasViewGuidelineThickness, DEFAULT_GUIDELINE_THICKNESS);
            this.mGuidelineCornerThickness = ta.getDimension(R.styleable.CanvasView_canvasViewGuidelineCornerThickness, DEFAULT_GUIDELINE_CORNER_THICKNESS);
            this.mGuidelineCornerLength = ta.getDimension(R.styleable.CanvasView_canvasViewGuidelineCornerLength, DEFAULT_GUIDELINE_CORNER_LENGTH);
            // corner paint
            this.mCornerPaint.setColor(ta.getColor(R.styleable.CanvasView_canvasViewGuidelineCornerColor, Color.WHITE));
            this.mCornerPaint.setStrokeWidth(this.mGuidelineCornerThickness);
            // border paint
            this.mBorderPaint.setColor(ta.getColor(R.styleable.CanvasView_canvasViewGuidelineBorderColor, Color.WHITE));
            this.mBorderPaint.setStrokeWidth(this.mGuidelineBorderThickness);
            // guideline paint
            this.mGuidelinePaint.setColor(ta.getColor(R.styleable.CanvasView_canvasViewGuidelineColor, Color.WHITE));
            this.mGuidelinePaint.setStrokeWidth(this.mGuidelineThickness);
            // scale
            this.mScaleRate = ta.getFloat(R.styleable.CanvasView_canvasViewScaleRate, DEFAULT_SCALE_RATE);
            this.mMaxScale = ta.getFloat(R.styleable.CanvasView_canvasViewMaxScale, DEFAULT_MAX_SCALE);

            ta.recycle();
        } else {
            this.mGestureEnabled = DEFAULT_GESTURE_ENABLED;
            this.mGuidelineVisibility = DEFAULT_GUIDELINE_VISIBILITY;
            this.mGuidelineBorderThickness = DEFAULT_GUIDELINE_BORDER_THICKNESS;
            this.mGuidelineColumns= DEFAULT_GUIDELINE_COLUMNS;
            this.mGuidelineRows = DEFAULT_GUIDELINE_ROWS;
            this.mGuidelineThickness = DEFAULT_GUIDELINE_THICKNESS;
            this.mGuidelineCornerThickness = DEFAULT_GUIDELINE_CORNER_THICKNESS;
            this.mGuidelineCornerLength = DEFAULT_GUIDELINE_CORNER_LENGTH;
            this.mBorderPaint.setColor(Color.WHITE);
            this.mCornerPaint.setColor(Color.WHITE);
            this.mGuidelinePaint.setColor(Color.WHITE);
            this.mScaleRate = DEFAULT_SCALE_RATE;
            this.mMaxScale = DEFAULT_MAX_SCALE;
        }

        this.setScaleType(ScaleType.MATRIX);
    }

    /**
     * Returns a boolean value to indicate whether gesture detection is enabled or disabled
     *
     * @return true if gesture detection enabled, otherwise false is returned
     */
    public boolean isGestureEnabled() {
        return this.mGestureEnabled;
    }

    /**
     * Sets gesture detection enabled or disabled
     *
     * @param enabled
     *            A boolean value to indicate whether gesture detection is enabled or disabled
     */
    public void setGestureEnabled(final boolean enabled) {
        this.mGestureEnabled = enabled;
    }

    /**
     * Returns a boolean value to indicate the visibility of guideline
     *
     * @return true if the guideline is visible, otherwise false is returned
     */
    public boolean isGuidelineVisible() {
        return this.mGuidelineVisibility;
    }

    /**
     * Sets the visibility of the guideline
     *
     * @param visible
     *            The visibility of guideline
     */
    public void setGuidelineVisible(final boolean visible) {
        this.mGuidelineVisibility = visible;
        invalidate();
        final Matrix m = getImageViewMatrix();
        final PointF t = getTranslation(m);
    }

    /**
     * Returns the number of columns of guideline
     *
     * @return the number of columns of guideline
     */
    public int getGuidelineColumns() {
        return this.mGuidelineColumns;
    }

    /**
     * Sets the number of columns of guideline
     *
     * @param columns
     *            The number of columns of guideline
     */
    public void setGuidelineColumns(final int columns) {
        this.mGuidelineColumns = columns;
        invalidate();
    }

    /**
     * Returns the number of rows of guideline
     *
     * @return the number of rows of guideline
     */
    public int getGuidelineRows() {
        return this.mGuidelineRows;
    }

    /**
     * Sets the number of rows of guideline
     *
     * @param rows
     *            The number of rows of guideline
     */
    public void setGuidelineRows(final int rows) {
        this.mGuidelineRows = rows;
        invalidate();
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        if (this.mGuidelineVisibility) {
            canvas.translate(getPaddingLeft(), getPaddingTop());
            this.drawGuideline(canvas);
            this.drawGuidelineBorder(canvas);
            this.drawGuidelineLeftTopCorner(canvas);
            this.drawGuidelineRightTopCorner(canvas);
            this.drawGuidelineRightBottomCorner(canvas);
            this.drawGuidelineLeftBottomCorner(canvas);
            canvas.translate(-getPaddingLeft(), -getPaddingTop());
        }
    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        this.mThisWidth = right - left;
        this.mThisHeight = bottom - top;

        final Runnable r = this.mOnLayoutRunnable;
        if (null != r) {
            mOnLayoutRunnable = null;
            r.run();
        }

        if (null != this.mBitmapDisplayed.getBitmap()) {
            getProperBaseMatrix(this.mBitmapDisplayed, this.mBaseMatrix);
            setImageMatrix(getImageViewMatrix());
        }
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if ((!isEnabled()) || (!this.mGestureEnabled)) {
            return super.onTouchEvent(event);
        }

        this.mDragGestureDetector.onTouchEvent(event);
        this.mScaleGestureDetector.onTouchEvent(event);
        this.mRotateGestureDetector.onTouchEvent(event);
        return true;
    }

    /**
     * Returns the snapshot of this canvas without guidelines
     *
     * @return a snapshot bitmap
     */
    public Bitmap snapshot() {
        final Bitmap snapshot;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            snapshot = Bitmap.createBitmap(getResources().getDisplayMetrics(), getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        } else {
            snapshot = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        }

        final boolean visibility = this.mGuidelineVisibility;
        final Canvas canvas = new Canvas(snapshot);
        this.mGuidelineVisibility = false;
        this.draw(canvas);
        this.mGuidelineVisibility = visibility;
        return snapshot;
    }

    /**
     * Sets the bitmap recycler
     *
     * @param recycler
     *            The bitmap recycler
     */
    public void setRecycler(final Recycler recycler) {
        this.mRecycler = recycler;
    }

    @Override
    public void setImageBitmap(final Bitmap bitmap) {
        this.setImageBitmap(bitmap, 0);
    }

    /**
     * Clear this canvas
     */
    public void clear() {
        setImageBitmapResetBase(null, true);
    }

    /**
     * Returns the scale of canvas
     *
     * @return the scale of canvas
     */
    public float getScale() {
        return this.getScale(this.mSupplementaryMatrix);
    }

    /**
     * Returns the scale of the specified matrix
     *
     * @param matrix
     *            A matrix
     * @return the scale of the specified matrix
     */
    protected float getScale(final Matrix matrix) {
        final float scaleX = getValue(matrix, Matrix.MSCALE_X);
        final float skewY = getValue(matrix, Matrix.MSKEW_Y);
        return (float) Math.sqrt(scaleX * scaleX + skewY * skewY);
    }

    /**
     * Returns the rotation of the canvas
     *
     * @return the rotation of the canvas
     */
    public float getRotation() {
        return this.getRotation(this.mSupplementaryMatrix);
    }

    /**
     * Returns the rotation of the specified matrix
     *
     * @param matrix
     *            A matrix
     * @return the rotation of the specified matrix
     */
    protected float getRotation(final Matrix matrix) {
        final float scaleX = getValue(matrix, Matrix.MSCALE_X);
        final float skewX  = getValue(matrix, Matrix.MSKEW_X);
        return Math.round(Math.atan2(skewX, scaleX) * (180 / Math.PI));
    }

    /**
     * Returns the translation value
     *
     * @return the translation value
     */
    public PointF getTranslation() {
        return this.getTranslation(this.mSupplementaryMatrix);
    }

    /**
     * Returns the translation value of the specified matrix
     *
     * @param matrix
     *            A matrix
     * @return the translation value
     */
    protected PointF getTranslation(final Matrix matrix) {
        return new PointF(getValue(matrix, Matrix.MTRANS_X), getValue(matrix, Matrix.MTRANS_Y));
    }

    /**
     * Returns the specified value of the specified matrix
     *
     * @param matrix
     *            A matrix
     * @param index
     *            The value index
     * @return the value of the specified index
     */
    protected float getValue(final Matrix matrix, final int index) {
        matrix.getValues(this.mValues);
        return this.mValues[index];
    }

    private void setImageBitmapResetBase(final Bitmap bitmap, final boolean reset) {
        setImageRotateBitmapResetBase(new RotateBitmap(bitmap), reset);
    }

    private void setImageRotateBitmapResetBase(final RotateBitmap bitmap, final boolean reset) {
        final int viewWidth = getWidth();

        if (viewWidth <= 0) {
            this.mOnLayoutRunnable = new Runnable() {
                @Override
                public void run() {
                    setImageRotateBitmapResetBase(bitmap, reset);
                }
            };
            return;
        }

        if (null != bitmap.getBitmap()) {
            getProperBaseMatrix(bitmap, this.mBaseMatrix);
            setImageBitmap(bitmap.getBitmap(), bitmap.getRotation());
        } else {
            this.mBaseMatrix.reset();
            setImageBitmap(null);
        }

        if (reset) {
            this.mSupplementaryMatrix.reset();
        }

        setImageMatrix(getImageViewMatrix());
    }

    /**
     * Sets the displayed bitmap with the specified rotation
     *
     * @param bitmap
     *            The bitmap to display
     * @param rotation
     *            The rotation degree
     */
    protected void setImageBitmap(final Bitmap bitmap, final int rotation) {
        super.setImageBitmap(bitmap);
        final Drawable drawable = getDrawable();
        if (null != drawable) {
            drawable.setDither(true);
        }

        final Bitmap old = this.mBitmapDisplayed.getBitmap();
        this.mBitmapDisplayed.setBitmap(bitmap);
        this.mBitmapDisplayed.setRotation(rotation);

        if (old != null && old != bitmap && this.mRecycler != null) {
            this.mRecycler.recycle(old);
        }

        this.mOnLayoutRunnable = new Runnable() {
            @Override
            public void run() {
                zoomToFill();
            }
        };
    }

    /**
     * Scales the displayed image, with a pivot point at the center of canvas.
     *
     * @param scale
     *            The scale size
     */
    public void scale(final float scale) {
        this.scale(scale, getWidth() / 2f, getHeight() / 2f);
    }

    /**
     * Scales the displayed image, with a pivot point at (x, y)
     *
     * @param scale
     *            The scale size
     * @param x
     *            The pivot point in x-axis
     *
     * @param y
     *            The pivot point in y-axis
     */
    public void scale(final float scale, final float x, final float y) {
        final float oldScale = getScale();
        final float deltaScale = scale / oldScale;
        this.mSupplementaryMatrix.postScale(deltaScale, deltaScale, x, y);
        this.setImageMatrix(getImageViewMatrix());
    }

    /**
     * Scales the displayed image in the specified duration, with a pivot point at (x, y)
     *
     * @param scale
     *            The scale size
     * @param x
     *            The pivot point in x-axis
     * @param y
     *            The pivot point in y-axis
     * @param duration
     *            The duration in milliseconds
     */
    public void scale(final float scale, final float x, final float y, final float duration) {
        final float increment = (scale - getScale()) / duration;
        final float oldScale = getScale();
        final long startTime = System.currentTimeMillis();

        this.post(new Runnable() {
            @Override
            public void run() {
                final long now = System.currentTimeMillis();
                final float current = Math.min(duration, now - startTime);
                final float target = oldScale + (increment * current);

                scale(target, x, y);

                if (current < duration) {
                    post(this);
                }
            }
        });
    }

    /**
     * Moves the displayed image with the specified translation
     *
     * @param dx
     *            The translation in x-axis
     * @param dy
     *            The translation in y-axis
     */
    public void move(final float dx, final float dy) {
        this.mSupplementaryMatrix.postTranslate(dx, dy);
        this.setImageMatrix(getImageViewMatrix());
    }

    /**
     * Rotates the displayed image
     *
     * @param degree
     *            The degree to rotate
     * @param x
     *            The central point of in x-aix
     * @param y
     */
    public void rotate(final float degree, final float x, final float y) {
        this.mSupplementaryMatrix.postRotate(degree, x, y);
        this.setImageMatrix(getImageViewMatrix());
    }

    /**
     * Zooms the displayed image to fill this canvas
     */
    public void zoomToFill() {
        if (null == this.mBitmapDisplayed.getBitmap()) {
            return;
        }

        final float vw = this.getMeasuredWidth();
        final float vh = this.getMeasuredHeight();
        final float dw = this.mBitmapDisplayed.getWidth();
        final float dh = this.mBitmapDisplayed.getHeight();
        final float scale;

        if (dw * vh > vw * dh) {
            scale = vh / dh;
        } else {
            scale = vw / dw;
        }

        this.scale(scale, 0, 0);
        this.move((vw - dw * scale) / 2f, (vh - dh * scale) / 2f);
    }

    protected float getMaxZoom() {
        if (null == this.mBitmapDisplayed.getBitmap()) {
            return 1f;
        }

        final float fw = (float) this.mBitmapDisplayed.getWidth() / this.mThisWidth;
        final float fh = this.mBitmapDisplayed.getHeight() / this.mThisHeight;
        return Math.max(fw, fh) * this.mMaxScale;
    }

    /**
     * Zooms in the displayed image
     */
    public void zoomIn() {
        zoomIn(this.mScaleRate);
    }

    /**
     * Zooms out the displayed image
     */
    public void zoomOut() {
        zoomOut(this.mScaleRate);
    }

    /**
     * Zooms in the displayed image with the specified rate
     *
     * @param rate
     *            The zooming rate
     */
    public void zoomIn(final float rate) {
        if (null == this.mBitmapDisplayed.getBitmap()) {
            return;
        }

        this.mSupplementaryMatrix.postScale(rate, rate, getWidth() / 2f, getHeight() / 2f);

        setImageMatrix(getImageViewMatrix());
    }

    /**
     * Zooms out the displayed image with the specified rate
     *
     * @param rate
     *            The zooming rate
     */
    public void zoomOut(final float rate) {
        if (null == this.mBitmapDisplayed.getBitmap()) {
            return;
        }

        final float x = getWidth() / 2f;
        final float y = getHeight() / 2f;
        final Matrix matrix = new Matrix(this.mSupplementaryMatrix);
        matrix.postScale(1f / rate, 1f / rate, x, y);

        if (getScale(matrix) < 1f) {
            this.mSupplementaryMatrix.setScale(1f, 1f, x, y);
        } else {
            this.mSupplementaryMatrix.postScale(1f / rate, 1f / rate, x, y);
        }

        setImageMatrix(getImageViewMatrix());
        center(true, true);
    }

    /**
     * Moves the displayed image to the center of canvas
     *
     * @param horizontal
     *            Center in horizontal
     * @param vertical
     *            Center in vertical
     */
    protected void center(final boolean horizontal, final boolean vertical) {
        if (null == this.mBitmapDisplayed.getBitmap()) {
            return;
        }

        final Bitmap bmp = this.mBitmapDisplayed.getBitmap();
        final Matrix matrix = getImageViewMatrix();
        final RectF rect = new RectF(0, 0, bmp.getWidth(), bmp.getHeight());
        final float width = rect.width();
        final float height = rect.height();
        matrix.mapRect(rect);

        float deltaX = 0;
        float deltaY = 0;

        if (vertical) {
            final int viewHeight = getHeight();
            if (height < viewHeight) {
                deltaY = (viewHeight - height) / 2.0f - rect.top;
            } else if (rect.top > 0) {
                deltaY = -rect.top;
            } else if (rect.bottom < viewHeight) {
                deltaY = getHeight() - rect.bottom;
            }
        }

        if (horizontal) {
            final int viewWidth = getWidth();
            if (width < viewWidth) {
                deltaX = (viewWidth - width) / 2.0f - rect.left;
            } else if (rect.left > 0) {
                deltaX = -rect.left;
            } else if (rect.right < viewWidth) {
                deltaX = viewWidth - rect.right;
            }
        }

        this.mSupplementaryMatrix.postTranslate(deltaX, deltaY);

        setImageMatrix(getImageViewMatrix());
    }

    private Matrix getImageViewMatrix() {
        this.mDisplayMatrix.set(this.mBaseMatrix);
        this.mDisplayMatrix.postConcat(mSupplementaryMatrix);
        return this.mDisplayMatrix;
    }

    private void getProperBaseMatrix(final RotateBitmap bitmap, final Matrix matrix) {
        matrix.reset();
        matrix.postConcat(bitmap.getRotateMatrix());
    }

    private void drawGuideline(final Canvas canvas) {
        canvas.translate(this.mGuidelineCornerThickness, this.mGuidelineCornerThickness);

        final float half = Math.max(1, this.mGuidelineThickness / 2.0f);
        final float width = getWidth() - getPaddingLeft() - getPaddingRight() - this.mGuidelineCornerThickness - this.mGuidelineCornerThickness;
        final float height = getHeight() - getPaddingTop() - getPaddingBottom() - this.mGuidelineCornerThickness - this.mGuidelineCornerThickness;

        // draw horizontal guidelines
        if (this.mGuidelineRows > 1) {
            final float avgH = (height - ((this.mGuidelineRows - 1) * this.mGuidelineThickness)) / this.mGuidelineRows;

            for (int i = 1; i <= this.mGuidelineRows; i++) {
                final float h = avgH * i + (i - 1) * this.mGuidelineThickness + half;
                canvas.drawLine(0, h, width, h, this.mGuidelinePaint);
            }
        }

        // draw vertical guidelines
        if (this.mGuidelineColumns > 1) {
            final float avgW = (width - ((this.mGuidelineColumns - 1) * this.mGuidelineThickness)) / this.mGuidelineColumns;

            for (int i = 1; i <= this.mGuidelineColumns; i++) {
                final float w = avgW * i + (i - 1) * this.mGuidelineThickness + half;
                canvas.drawLine(w, 0, w, height, this.mGuidelinePaint);
            }
        }

        canvas.translate(-this.mGuidelineCornerThickness, -this.mGuidelineCornerThickness);
    }

    private void drawGuidelineBorder(final Canvas canvas) {
        final int width = getWidth() - getPaddingLeft() - getPaddingRight();
        final int height = getHeight() - getPaddingTop() - getPaddingBottom();
        final float half = Math.max(1, this.mGuidelineBorderThickness / 2.0f);
        canvas.drawRect(this.mGuidelineCornerThickness - half, this.mGuidelineCornerThickness - half, width - this.mGuidelineCornerThickness + half, height - this.mGuidelineCornerThickness + half, this.mBorderPaint);
    }

    private void drawGuidelineLeftTopCorner(final Canvas canvas) {
        canvas.drawRect(0, 0, this.mGuidelineCornerThickness + this.mGuidelineCornerLength, this.mGuidelineCornerThickness, this.mCornerPaint);
        canvas.drawRect(0, 0, this.mGuidelineCornerThickness, this.mGuidelineCornerThickness + this.mGuidelineCornerLength, this.mCornerPaint);
    }

    private void drawGuidelineRightTopCorner(final Canvas canvas) {
        final int width = getWidth() - getPaddingLeft() - getPaddingRight();
        canvas.drawRect(width - this.mGuidelineCornerLength - this.mGuidelineCornerThickness, 0, width, this.mGuidelineCornerThickness, this.mCornerPaint);
        canvas.drawRect(width - this.mGuidelineCornerThickness, 0, width, this.mGuidelineCornerThickness + this.mGuidelineCornerLength, this.mCornerPaint);
    }

    private void drawGuidelineRightBottomCorner(final Canvas canvas) {
        final int width = getWidth() - getPaddingLeft() - getPaddingRight();
        final int height = getHeight() - getPaddingTop() - getPaddingBottom();
        canvas.drawRect(width - this.mGuidelineCornerThickness, height - this.mGuidelineCornerThickness - this.mGuidelineCornerLength, width, height, this.mCornerPaint);
        canvas.drawRect(width - this.mGuidelineCornerThickness - this.mGuidelineCornerLength, height - this.mGuidelineCornerThickness, width, height, this.mCornerPaint);
    }

    private void drawGuidelineLeftBottomCorner(final Canvas canvas) {
        final int height = getHeight() - getPaddingTop() - getPaddingBottom();
        canvas.drawRect(0, height - this.mGuidelineCornerThickness - this.mGuidelineCornerLength, this.mGuidelineCornerThickness, height, this.mCornerPaint);
        canvas.drawRect(0, height - this.mGuidelineCornerThickness, this.mGuidelineCornerThickness + this.mGuidelineCornerLength, height, this.mCornerPaint);
    }

    /**
     * Bitmap recycler
     */
    public interface Recycler {
        /**
         * Recycle the specified bitmap
         *
         * @param bitmap
         *            The bitmap to recycle
         */
        void recycle(final Bitmap bitmap);
    }

    private final class GestureListener implements DragGestureDetector.OnDragGestureListener, ScaleGestureDetector.OnScaleGestureListener, RotateGestureDetector.OnRotateListener {

        @Override
        public boolean onRotateBegin(final RotateGestureDetector detector) {
            return true;
        }

        @Override
        public boolean onRotate(final RotateGestureDetector detector) {
            final PointF o = detector.getPivot();
            rotate(detector.getDeltaRotation(), o.x, o.y);
            return true;
        }

        @Override
        public void onRotateEnd(final RotateGestureDetector detector) {
        }

        @Override
        public boolean onScale(final ScaleGestureDetector detector) {
            final float scale = detector.getScaleFactor();
            if (Float.isInfinite(scale) || Float.isNaN(scale)) {
                return true;
            }

            scale(getScale() * detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
            return true;
        }

        @Override
        public boolean onScaleBegin(final ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(final ScaleGestureDetector detector, final boolean cancel) {
        }

        @Override
        public boolean onDragBegin(final DragGestureDetector detector) {
            return true;
        }

        @Override
        public boolean onDrag(final DragGestureDetector detector) {
            move(detector.getDeltaX(), detector.getDeltaY());
            return true;
        }

        @Override
        public void onDragEnd(final DragGestureDetector detector, final boolean cancel) {
        }
    }

    private static final class RotateBitmap {
        private Bitmap mBitmap;
        private int mRotation;

        public RotateBitmap(final Bitmap bitmap) {
            this(bitmap, 0);
        }

        public RotateBitmap(final Bitmap bitmap, final int rotation) {
            this.setBitmap(bitmap);
            this.setRotation(rotation);
        }

        public Bitmap getBitmap() {
            return this.mBitmap;
        }

        public int getRotation() {
            return this.mRotation;
        }

        public void setBitmap(final Bitmap bitmap) {
            this.mBitmap = bitmap;
        }

        public void setRotation(final int rotation) {
            this.mRotation = (rotation + 360) % 360;
        }

        public Matrix getRotateMatrix() {
            final Matrix matrix = new Matrix();

            if (this.mRotation != 0 && this.mBitmap != null) {
                final float centerX = this.mBitmap.getWidth() / 2.0f;
                final float centerY = this.mBitmap.getHeight() / 2.0f;
                matrix.preTranslate(-centerX, -centerY);
                matrix.postRotate(this.mRotation);
                matrix.postTranslate(getWidth() / 2.0f, getHeight() / 2.0f);
            }

            return matrix;
        }

        public boolean isOrientationChanged() {
            return (this.mRotation / 90) % 2 != 0;
        }

        public int getHeight() {
            return isOrientationChanged() ? this.mBitmap.getWidth() : this.mBitmap.getHeight();
        }

        public int getWidth() {
            return isOrientationChanged() ? this.mBitmap.getHeight() : this.mBitmap.getWidth();
        }

        public void recycle() {
            if (null != this.mBitmap) {
                this.mBitmap.recycle();
                this.mBitmap = null;
            }
        }

    }

}
