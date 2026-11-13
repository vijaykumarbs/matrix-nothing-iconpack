package dev.vijaykumarbs.matrix;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.TreeSet;

/**
 * Single-screen control room: signature preview, style studio, apply guide,
 * icon library and icon requests. No permissions, no network, no background work.
 */
public final class MainActivity extends Activity {

    private static final int[] ACCENTS = {
            0xFF3BF5A0, 0xFFFF5540, 0xFFE9F0F6, 0xFFFFB454,
    };
    private static final String[] ACCENT_NAMES = {"Phosphor", "Ember", "Ice", "Amber"};

    private static final String[] HERO = {
            "gmail", "whatsapp", "instagram", "spotify",
            "youtube", "gmaps", "gcamera", "gmessages",
            "settings", "gphotos", "gdrive", "chrome",
    };

    private static final String[] LAUNCHERS = {
            "Nova Launcher", "Lawnchair", "Smart Launcher 6",
            "Niagara Launcher", "Hyperion Launcher", "Kvaesitso",
    };
    private static final String[] LAUNCHER_IDS = {
            "com.teslacoilsw.launcher", "app.lawnchair", "ginlemon.flowerfree",
            "bitpit.launcher", "app.hyperion.launcher", "de.mm20.launcher2.release",
    };

    private SharedPreferences prefs;
    private int accentIndex;
    private boolean glass;

    private StyleMarkView styleMark;
    private TextView styleName;
    private TextView styleState;
    private TextView styleAccentName;
    private TextView btnInstall;
    private TextView segGlass;
    private TextView segMatte;
    private TextView btnRequest;
    private SwatchView[] swatches;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.edgeToEdge(this);
        setContentView(R.layout.activity_main);

        View scroll = findViewById(R.id.scroll);
        Ui.padForSystemBars(scroll);

        prefs = getSharedPreferences("studio", MODE_PRIVATE);
        Styles.Style installed = Styles.current();
        int defaultAccent = Math.max(0, accentIndex(installed.accent));
        accentIndex = prefs.getInt("accent", defaultAccent);
        glass = prefs.getBoolean("glass", installed.glass);
        if (accentIndex < 0 || accentIndex >= ACCENTS.length) accentIndex = defaultAccent;

        styleMark = findViewById(R.id.styleMark);
        styleName = findViewById(R.id.styleName);
        styleState = findViewById(R.id.styleState);
        styleAccentName = findViewById(R.id.styleAccentName);
        btnInstall = findViewById(R.id.btnInstall);
        segGlass = findViewById(R.id.segGlass);
        segMatte = findViewById(R.id.segMatte);
        btnRequest = findViewById(R.id.btnRequest);

        findViewById(R.id.btnApply).setOnClickListener(v -> showApplyDialog());
        findViewById(R.id.btnLaunchers).setOnClickListener(v -> showLaunchers());
        findViewById(R.id.libraryCard).setOnClickListener(v ->
                startActivity(new Intent(this, IconLibraryActivity.class)));
        findViewById(R.id.linkRepo).setOnClickListener(v -> openUrl(BuildConfig.REPO_URL));
        btnRequest.setOnClickListener(v -> buildIconRequest());

        buildHero();
        buildSwatches();

        segGlass.setOnClickListener(v -> setGlass(true));
        segMatte.setOnClickListener(v -> setGlass(false));

        TextView steps = findViewById(R.id.applySteps);
        steps.setText(getString(R.string.apply_steps, BuildConfig.STYLE_NAME));

        TextView meta = findViewById(R.id.heroMeta);
        meta.setText(getString(R.string.hero_meta).replace("126", String.valueOf(IconCatalog.COUNT)));

        syncStudio();
    }

    private void buildHero() {
        LinearLayout grid = findViewById(R.id.heroGrid);
        int size = Ui.dp(this, 56);
        int gap = Ui.dp(this, 8);
        for (int row = 0; row < 3; row++) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER_HORIZONTAL);
            if (row > 0) {
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.topMargin = gap;
                grid.addView(line, rowParams);
            } else {
                grid.addView(line);
            }
            for (int col = 0; col < 4; col++) {
                IconTileView tile = new IconTileView(this);
                tile.setIconRes(resFor(HERO[row * 4 + col]));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.setMarginStart(col == 0 ? 0 : gap);
                line.addView(tile, params);
            }
        }
    }

    private void buildSwatches() {
        LinearLayout row = findViewById(R.id.swatchRow);
        swatches = new SwatchView[ACCENTS.length];
        int size = Ui.dp(this, 46);
        int gap = Ui.dp(this, 8);
        for (int i = 0; i < ACCENTS.length; i++) {
            final int index = i;
            SwatchView swatch = new SwatchView(this);
            swatch.setAccent(ACCENTS[i]);
            swatch.setContentDescription(ACCENT_NAMES[i] + " dot colour");
            swatch.setOnClickListener(v -> selectAccent(index));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMarginEnd(gap);
            row.addView(swatch, params);
            swatches[i] = swatch;
        }
    }

    private void selectAccent(int index) {
        accentIndex = index;
        prefs.edit().putInt("accent", index).apply();
        syncStudio();
    }

    private void setGlass(boolean enabled) {
        glass = enabled;
        prefs.edit().putBoolean("glass", enabled).apply();
        syncStudio();
    }

    private void syncStudio() {
        Styles.Style style = styleFor(accentIndex, glass);
        int accent = ACCENTS[accentIndex];

        styleMark.setAccent(accent);
        styleMark.setGlass(glass);
        styleName.setText(style.name);
        styleAccentName.setText(ACCENT_NAMES[accentIndex].toUpperCase() + " · "
                + (glass ? "GLASS" : "MATTE"));

        boolean installed = style.id.equals(BuildConfig.STYLE_ID);
        styleState.setText(installed ? R.string.studio_installed : R.string.studio_available);
        styleState.setTextColor(installed ? accent : getColor(R.color.text_dim));
        styleState.setBackgroundResource(installed ? R.drawable.bg_chip_on : R.drawable.bg_chip);

        for (int i = 0; i < swatches.length; i++) {
            swatches[i].setSelectedSwatch(i == accentIndex);
        }

        segGlass.setBackgroundResource(glass ? R.drawable.bg_segment_on : R.drawable.bg_segment);
        segGlass.setTextColor(glass ? getColor(R.color.on_accent) : getColor(R.color.text_dim));
        segMatte.setBackgroundResource(glass ? R.drawable.bg_segment : R.drawable.bg_segment_on);
        segMatte.setTextColor(glass ? getColor(R.color.text_dim) : getColor(R.color.on_accent));

        TextView cta = btnInstall;
        cta.setText(installed
                ? getString(R.string.studio_apply)
                : "GET " + style.name.toUpperCase());
        cta.setOnClickListener(v -> {
            if (style.id.equals(BuildConfig.STYLE_ID)) {
                showApplyDialog();
            } else {
                openUrl(BuildConfig.REPO_URL + "/releases/latest/download/" + style.apk);
            }
        });
    }

    private void showApplyDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.apply_dialog_title)
                .setMessage(getString(R.string.apply_steps, BuildConfig.STYLE_NAME)
                        + "\n\n" + getString(R.string.apply_nothing))
                .setPositiveButton("LAUNCHER LINKS", (d, w) -> showLaunchers())
                .setNegativeButton("CLOSE", null)
                .show();
    }

    private void showLaunchers() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.apply_launchers)
                .setItems(LAUNCHERS, (d, which) -> openMarket(LAUNCHER_IDS[which]))
                .setNegativeButton("CLOSE", null)
                .show();
    }

    private void buildIconRequest() {
        btnRequest.setEnabled(false);
        btnRequest.setText("SCANNING…");
        new Thread(() -> {
            TreeSet<String> missing = new TreeSet<>();
            String error = null;
            try {
                PackageManager pm = getPackageManager();
                Intent launcher = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> apps = pm.queryIntentActivities(launcher, 0);
                for (ResolveInfo info : apps) {
                    String pkg = info.activityInfo.packageName;
                    if (pkg.startsWith("dev.vijaykumarbs.matrix")) continue;
                    if (ThemedPackages.contains(pkg)) continue;
                    missing.add(info.loadLabel(pm) + "\t" + pkg + "/" + info.activityInfo.name);
                }
            } catch (Exception e) {
                error = e.getClass().getSimpleName();
            }
            final TreeSet<String> result = missing;
            final String failure = error;
            runOnUiThread(() -> {
                btnRequest.setEnabled(true);
                btnRequest.setText(R.string.request_button);
                showRequestDialog(result, failure);
            });
        }, "icon-request").start();
    }

    private void showRequestDialog(TreeSet<String> apps, String error) {
        if (error != null) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.request_dialog_title)
                    .setMessage("Could not read the app list (" + error + ").")
                    .setPositiveButton("CLOSE", null)
                    .show();
            return;
        }
        if (apps.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.request_dialog_title)
                    .setMessage(R.string.request_none)
                    .setPositiveButton("CLOSE", null)
                    .show();
            return;
        }
        StringBuilder body = new StringBuilder();
        int shown = 0;
        for (String line : apps) {
            if (shown++ == 12) {
                body.append("…and ").append(apps.size() - 12).append(" more");
                break;
            }
            body.append(line).append('\n');
        }
        String payload = String.join("\n", apps);
        new AlertDialog.Builder(this)
                .setTitle(R.string.request_dialog_title)
                .setMessage(getString(R.string.request_count, apps.size()) + "\n\n" + body)
                .setPositiveButton(R.string.request_copy, (d, w) -> {
                    ClipboardManager clipboard =
                            (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(
                            ClipData.newPlainText("Matrix icon request", payload));
                    Toast.makeText(this, R.string.request_copied, Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton(R.string.request_open_issues, (d, w) ->
                        openUrl(BuildConfig.REPO_URL + "/issues/new?title=Icon%20request"))
                .setNegativeButton("CLOSE", null)
                .show();
    }

    private void openMarket(String pkg) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg)));
        } catch (Exception e) {
            openUrl("https://play.google.com/store/apps/details?id=" + pkg);
        }
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, url, Toast.LENGTH_LONG).show();
        }
    }

    private static int resFor(String name) {
        for (int i = 0; i < IconCatalog.NAMES.length; i++) {
            if (IconCatalog.NAMES[i].equals(name)) return IconCatalog.RES[i];
        }
        return 0;
    }

    private static int accentIndex(int color) {
        for (int i = 0; i < ACCENTS.length; i++) {
            if (ACCENTS[i] == color) return i;
        }
        return 0;
    }

    private static Styles.Style styleFor(int accentIndex, boolean glass) {
        for (Styles.Style style : Styles.ALL) {
            if (style.accent == ACCENTS[accentIndex] && style.glass == glass) return style;
        }
        return Styles.current();
    }
}
