package com.example.zebraview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by TMXKT on 2022/1/14
 */
public class ZebraView extends View {

    @IntDef({HORIZONTAL, VERTICAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface OrientationMode {
    }

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;
    private int orientation;
    private int interval = 10;
    private int lineWidth = 10;
    private int colorLine;
    private int colorLineStart;
    private int colorLineEnd;
    private int colorBg;
    private int colorBgStart;
    private int colorBgEnd;
    private boolean isMirror;
    private boolean isAngle;
    private int radius;
    private int radiusTopLeft;
    private int radiusTopRight;
    private int radiusBottomLeft;
    private int radiusBottomRight;

    private Paint paint;
    private RectF rectF;
    private Path path;

    private Bitmap bitmapLineBg = null;
    private Bitmap bitmapLine = null;

    private Bitmap bitmapContent = null;
    private Bitmap bitmapTargetShape = null;

    private ShapeDrawable shapeDrawable = null;
    PorterDuffXfermode pdXfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);

    public ZebraView(Context context) {
        this(context, null);
    }

    public ZebraView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZebraView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ZebraView);

        int index = ta.getInt(R.styleable.ZebraView_zbv_orientation, -1);
        if (index >= 0) {
            setOrientation(index);
        }
        interval = ta.getDimensionPixelSize(R.styleable.ZebraView_zbv_interval, interval);
        lineWidth = ta.getDimensionPixelSize(R.styleable.ZebraView_zbv_line_width, lineWidth);
        colorLine = ta.getColor(R.styleable.ZebraView_zbv_line_color, 0xFF465477);
        colorLineStart = ta.getColor(R.styleable.ZebraView_zbv_line_color_start, 0);
        colorLineEnd = ta.getColor(R.styleable.ZebraView_zbv_line_color_end, 0);

        colorBg = ta.getColor(R.styleable.ZebraView_zbv_bg_color, 0xFF2F3F65);
        colorBgStart = ta.getColor(R.styleable.ZebraView_zbv_bg_color_start, 0);
        colorBgEnd = ta.getColor(R.styleable.ZebraView_zbv_bg_color_end, 0);

        isMirror = ta.getBoolean(R.styleable.ZebraView_zbv_mirror, false);
        isAngle = ta.getBoolean(R.styleable.ZebraView_zbv_angle, true);

        radius = ta.getDimensionPixelSize(R.styleable.ZebraView_zbv_corner_radius, 0);
        radiusTopLeft = ta.getDimensionPixelSize(R.styleable.ZebraView_zbv_corner_radius_top_left, 0);
        radiusTopRight = ta.getDimensionPixelSize(R.styleable.ZebraView_zbv_corner_radius_top_right, 0);
        radiusBottomLeft = ta.getDimensionPixelSize(R.styleable.ZebraView_zbv_corner_radius_bottom_left, 0);
        radiusBottomRight = ta.getDimensionPixelSize(R.styleable.ZebraView_zbv_corner_radius_bottom_right, 0);

        ta.recycle();

        rectF = new RectF();
        paint = new Paint();
    }

    public void setOrientation(@OrientationMode int orientation) {
        if (this.orientation != orientation) {
            this.orientation = orientation;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = measureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight(),orientation == HORIZONTAL ? 160 : 60);
        int height = measureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom(),orientation == HORIZONTAL ? 60 : 160);
        setMeasuredDimension(width + getPaddingLeft() + getPaddingRight(), height + getPaddingTop() + getPaddingBottom());
    }

    protected int measureSpec(int measureSpec, int padding, int defaultSize) {
        int result = defaultSize;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                break;
            case MeasureSpec.AT_MOST:
                result = Math.min(result + padding, specSize + padding);
                break;
            case MeasureSpec.EXACTLY:
                result = specSize - padding;
                if (result < 0) result = 0;
                break;
        }
        return result;
    }

    @Override
    public void invalidate() {
        super.invalidate();
    }

    @Override
    public void requestLayout() {
        bitmapRecycle(bitmapContent);
        bitmapContent = null;
        bitmapRecycle(bitmapTargetShape);
        bitmapTargetShape = null;
        bitmapRecycle(bitmapLineBg);
        bitmapLineBg = null;
        bitmapRecycle(bitmapLine);
        bitmapLine = null;
        shapeDrawable = null;
        super.requestLayout();
    }

    private void bitmapRecycle(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        int contentWidth = width - paddingLeft - paddingRight;
        int contentHeight = height - paddingTop - paddingBottom;

        if (contentWidth == 0 || contentHeight == 0) {
            return;
        }

        if (bitmapContent == null || (bitmapContent.getWidth() != contentWidth || bitmapContent.getHeight() != contentHeight)) {
            bitmapRecycle(bitmapContent);
            bitmapContent = makeContent(contentWidth, contentHeight);
        }

        if (bitmapTargetShape == null || (bitmapTargetShape.getWidth() != contentWidth || bitmapTargetShape.getHeight() != contentHeight)) {
            bitmapRecycle(bitmapTargetShape);
            bitmapTargetShape = makeShape(contentWidth, contentHeight);
        }

        paint.reset();
        paint.setAntiAlias(true);
        int saveLayer = canvas.saveLayer(0, 0, width, height, null, Canvas.ALL_SAVE_FLAG);
        canvas.drawBitmap(bitmapTargetShape, paddingLeft, paddingTop, paint);
        paint.setXfermode(pdXfermode);
        canvas.drawBitmap(bitmapContent, paddingLeft, paddingTop, paint);
        paint.setXfermode(null);
        canvas.restoreToCount(saveLayer);
    }

    private Bitmap makeContent(int w, int h) {
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        checkMirror(w, h, c);

        paint.reset();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        if (colorBgStart != 0 || colorBgEnd != 0) {
            LinearGradient linearGradient = getLinearGradient(w, h, colorBgStart, colorBgEnd);
            paint.setShader(linearGradient);
        } else {
            paint.setColor(colorBg);
        }
        rectF.set(0, 0, w, h);
        c.drawRect(rectF, paint);

        paint.reset();
        paint.setAntiAlias(true);
        int sLayer = c.saveLayer(0, 0, w, h, null, Canvas.ALL_SAVE_FLAG);
        if (bitmapLineBg == null || (bitmapLineBg.getWidth() != w || bitmapLineBg.getHeight() != h)) {
            bitmapLineBg = null;
            bitmapLineBg = makeContentLineBg(w, h);
        }
        if (bitmapLine == null || (bitmapLine.getWidth() != w || bitmapLine.getHeight() != h)) {
            bitmapLine = null;
            bitmapLine = makeContentLine(w, h);
        }
        c.drawBitmap(bitmapLine, 0, 0, paint);
        paint.setXfermode(pdXfermode);
        c.drawBitmap(bitmapLineBg, 0, 0, paint);
        paint.setXfermode(null);
        c.restoreToCount(sLayer);

        return bm;
    }

    private Bitmap makeContentLineBg(int w, int h) {
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);

        paint.reset();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        if (colorLineStart != 0 && colorLineEnd != 0) {
            LinearGradient linearGradient = getLinearGradient(w, h, colorLineStart, colorLineEnd);
            paint.setShader(linearGradient);
        } else {
            paint.setColor(colorLine);
            paint.setShader(null);
        }
        rectF.set(0, 0, w, h);
        c.drawRect(rectF, paint);

        return bm;
    }

    private Bitmap makeContentLine(int w, int h) {
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);

        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1);

        int count = 0;
        int offset = lineWidth + interval;
        if (isAngle) {
            int bottomFirstX = interval - h;
            while (bottomFirstX + (count * offset) < w) {
                getAnglePath(h, count, offset, bottomFirstX);
                c.drawPath(path, paint);
                count++;
            }
        } else {
            if (orientation == VERTICAL) {
                int firstX = interval;
                while (firstX + (count * offset) < w) {
                    getVerticalPath(h, count, offset, firstX);
                    c.drawPath(path, paint);
                    count++;
                }
            } else if (orientation == HORIZONTAL) {
                int firstY = interval;
                while (firstY + (count * offset) < h) {
                    getHorizontalPath(w, count, offset, firstY);
                    c.drawPath(path, paint);
                    count++;
                }
            }
        }

        return bm;
    }

    private Bitmap makeShape(int w, int h) {
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        float[] outerR;
        if (radius != 0) {
            outerR = new float[]{radius, radius, radius, radius, radius, radius, radius, radius};
        } else {
            outerR = new float[]{radiusTopLeft, radiusTopLeft, radiusTopRight, radiusTopRight, radiusBottomRight, radiusBottomRight, radiusBottomLeft, radiusBottomLeft};
        }

        if (shapeDrawable == null) {
            shapeDrawable = new ShapeDrawable(new RoundRectShape(outerR, null, null));
        } else {
            shapeDrawable.setShape(new RoundRectShape(outerR, null, null));
        }
        shapeDrawable.getPaint().setColor(Color.WHITE);
        shapeDrawable.getPaint().setAntiAlias(true);
        shapeDrawable.setBounds(0, 0, w, h);
        shapeDrawable.draw(c);

        return bm;
    }

    private void checkMirror(int w, int h, Canvas c) {
        if (orientation == HORIZONTAL && isMirror) {
            c.scale(1f, -1f, 0, h / 2);
        } else if (orientation == VERTICAL && isMirror) {
            c.scale(-1f, 1f, w / 2, 0);
        }
    }

    private LinearGradient getLinearGradient(int w, int h, int colorBgStart, int colorBgEnd) {
        return new LinearGradient(0, 0,
                orientation == HORIZONTAL ? w : 0,
                orientation == VERTICAL ? h : 0,
                colorBgStart,
                colorBgEnd,
                Shader.TileMode.CLAMP);
    }

    private void getAnglePath(int h, int count, int offset, int bottomFirstX) {
        path = null;
        path = new Path();
        path.moveTo(interval + (count * offset), 0);
        path.lineTo(bottomFirstX + (count * offset), h);
        path.lineTo(bottomFirstX + lineWidth + (count * offset), h);
        path.lineTo(bottomFirstX + lineWidth + h + (count * offset), 0);
        path.lineTo(bottomFirstX + h + (count * offset), 0);
        path.close();
    }

    private void getVerticalPath(int h, int count, int offset, int firstX) {
        path = null;
        path = new Path();
        path.moveTo(firstX + (count * offset), 0);
        path.lineTo(firstX + (count * offset), h);
        path.lineTo(firstX + lineWidth + (count * offset), h);
        path.lineTo(firstX + lineWidth + (count * offset), 0);
        path.lineTo(firstX + lineWidth - lineWidth + (count * offset), 0);
        path.close();
    }

    private void getHorizontalPath(int w, int count, int offset, int firstY) {
        path = null;
        path = new Path();
        path.moveTo(0, firstY + (count * offset));
        path.lineTo(w, firstY + (count * offset));
        path.lineTo(w, firstY + lineWidth + (count * offset));
        path.lineTo(0, firstY + lineWidth + (count * offset));
        path.lineTo(0, firstY + lineWidth - lineWidth + (count * offset));
        path.close();
    }
}
