package dev.vijaykumarbs.matrix;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Launcher-facing icon-pack entry point. Opening it takes the user straight to
 * the control room, which is where applying and style switching actually happen.
 */
public final class ThemeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
