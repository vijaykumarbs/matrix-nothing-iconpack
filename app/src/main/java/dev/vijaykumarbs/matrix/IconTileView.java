package dev.vijaykumarbs.matrix;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Draws an adaptive icon clipped to a squircle, the way a launcher would.
 * Uses the pack's own vector resources, so previews are always truthful.
 */
public final class IconTileView extends View {

    private final Path clip = new Path();
    private final RectF bounds = new RectF();
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Drawable icon;
    private float radius;

    public IconTileView(Context context) {
        this(context, null);
    }

    public IconTileView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconTileView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(Ui.dp(this, 1f));
        border.setColor(Color.argb(26, 255, 255, 255));
        setWillNotDraw(false);
    }

    public void setIcon(Drawable drawable) {
        icon = drawable;
        invalidate();
    }

    public void setIconRes(int resId) {
        Drawable d = resId == 0 ? null : getContext().getDrawable(resId);
        setIcon(d);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        int size = mode == MeasureSpec.EXACTLY ? w : Math.min(w, h);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float inset = 0f;
        bounds.set(inset, inset, w - inset, h - inset);
        radius = Math.min(bounds.width(), bounds.height()) * 0.235f;

        clip.reset();
        clip.addRoundRect(bounds, radius, radius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        if (icon != null) {
            icon.setBounds(0, 0, w, h);
            icon.draw(canvas);
        }
        canvas.restore();

        // hairline edge keeps the tile crisp on pure black wallpapers
        border.setStrokeWidth(Math.max(1f, Ui.dp(this, 0.75f)));
        canvas.drawRoundRect(bounds, radius, radius, border);
    }
}
