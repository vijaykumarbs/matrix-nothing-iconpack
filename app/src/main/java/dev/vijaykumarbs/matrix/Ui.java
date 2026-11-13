package dev.vijaykumarbs.matrix;

import android.app.Activity;
import android.graphics.Insets;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

/** Small, dependency-free UI helpers. */
final class Ui {

    static float dp(View v, float value) {
        return value * v.getResources().getDisplayMetrics().density;
    }

    static int dp(Activity a, float value) {
        return Math.round(value * a.getResources().getDisplayMetrics().density);
    }

    static void edgeToEdge(Activity activity) {
        Window window = activity.getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller =
                    window.getDecorView().getWindowInsetsController();
            if (controller != null) {
                // Dark app surface: force light status/nav icons on every OEM skin.
                controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    /** Pads a view so content clears system bars + display cutout. */
    static void padForSystemBars(View view) {
        final int start = view.getPaddingStart();
        final int top = view.getPaddingTop();
        final int end = view.getPaddingEnd();
        final int bottom = view.getPaddingBottom();
        view.setOnApplyWindowInsetsListener((v, insets) -> {
            int l, t, r, b;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                l = bars.left;
                t = bars.top;
                r = bars.right;
                b = bars.bottom;
            } else {
                l = insets.getSystemWindowInsetLeft();
                t = insets.getSystemWindowInsetTop();
                r = insets.getSystemWindowInsetRight();
                b = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(start + l, top + t, end + r, bottom + b);
            return insets;
        });
        view.requestApplyInsets();
    }

    static int withAlpha(int color, float alpha) {
        int a = Math.round(255f * Math.max(0f, Math.min(1f, alpha)));
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }

    private Ui() {
    }
}
