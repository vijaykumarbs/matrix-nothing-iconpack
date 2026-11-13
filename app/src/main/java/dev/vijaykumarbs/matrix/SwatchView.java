package dev.vijaykumarbs.matrix;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** Accent selector. Filled dot + halo when selected. */
public final class SwatchView extends View {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int accent = 0xFF3BF5A0;
    private boolean selected;

    public SwatchView(Context context) {
        this(context, null);
    }

    public SwatchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwatchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(Ui.dp(this, 1.5f));
    }

    public void setAccent(int color) {
        accent = color;
        fill.setColor(color);
        ring.setColor(color);
        invalidate();
    }

    public void setSelectedSwatch(boolean value) {
        selected = value;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.min(MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec));
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) / 2f - Ui.dp(this, 7f);

        fill.setAlpha(selected ? 255 : 120);
        canvas.drawCircle(cx, cy, radius, fill);

        if (selected) {
            ring.setAlpha(255);
            ring.setStrokeWidth(Ui.dp(this, 1.4f));
            canvas.drawCircle(cx, cy, radius + Ui.dp(this, 4.5f), ring);
        } else {
            ring.setAlpha(0);
        }

        if (selected) {
            fill.setColor(Color.argb(210, 5, 7, 10));
            canvas.drawCircle(cx, cy, Ui.dp(this, 2.6f), fill);
            fill.setColor(accent);
        }
    }
}
