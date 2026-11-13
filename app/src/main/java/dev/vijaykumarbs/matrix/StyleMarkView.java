package dev.vijaykumarbs.matrix;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/** Live preview of the tile + accent combination, drawn with a 13 x 13 dot mark. */
public final class StyleMarkView extends View {

    private static final String[] MARK = {
            "##...##",
            "###.###",
            "##.#.##",
            "##.#.##",
            "##...##",
            "##...##",
            "##...##",
            "##...##",
            "##...##",
    };
    private static final int GRID = 13;

    private final Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF rect = new RectF();

    private int accent = 0xFF3BF5A0;
    private boolean glass = true;

    public StyleMarkView(Context context) {
        this(context, null);
    }

    public StyleMarkView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StyleMarkView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        dot.setStyle(Paint.Style.FILL);
        edge.setStyle(Paint.Style.STROKE);
        edge.setColor(Color.argb(30, 255, 255, 255));
        setWillNotDraw(false);
    }

    public void setAccent(int color) {
        accent = color;
        dot.setColor(color);
        invalidate();
    }

    public void setGlass(boolean enabled) {
        glass = enabled;
        invalidate();
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

        float radius = Math.min(w, h) * 0.235f;
        rect.set(0, 0, w, h);
        clip.reset();
        clip.addRoundRect(rect, radius, radius, Path.Direction.CW);

        canvas.save();
        canvas.clipPath(clip);

        if (glass) {
            background.setShader(new LinearGradient(0, 0, w, h,
                    new int[]{0xFF191D20, 0xFF0C0E10, 0xFF07080A},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        } else {
            background.setShader(new LinearGradient(0, 0, 0, h,
                    new int[]{0xFF101214, 0xFF07080A}, null, Shader.TileMode.CLAMP));
        }
        canvas.drawRect(rect, background);

        if (glass) {
            glow.setShader(new RadialGradient(w * 0.26f, h * 0.18f, w * 0.95f,
                    new int[]{0x24FFFFFF, 0x0AFFFFFF, 0x00FFFFFF},
                    new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(rect, glow);

            int accentGlow = Ui.withAlpha(accent, 0.14f);
            glow.setShader(new RadialGradient(w * 0.92f, h * 0.96f, w * 0.85f,
                    new int[]{accentGlow, Ui.withAlpha(accent, 0.04f), 0x00000000},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(rect, glow);
        }

        float step = w * 0.062f;
        float dotRadius = w * 0.021f;
        float originX = w / 2f - step * (GRID - 1) / 2f;
        float originY = h / 2f - step * (GRID - 1) / 2f;
        int offX = (GRID - MARK[0].length()) / 2;
        int offY = (GRID - MARK.length) / 2;
        for (int row = 0; row < MARK.length; row++) {
            String line = MARK[row];
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == '#') {
                    canvas.drawCircle(
                            originX + (offX + col) * step,
                            originY + (offY + row) * step,
                            dotRadius, dot);
                }
            }
        }
        canvas.restore();

        edge.setStrokeWidth(Math.max(1f, Ui.dp(this, 0.75f)));
        canvas.drawRoundRect(rect, radius, radius, edge);
    }
}
