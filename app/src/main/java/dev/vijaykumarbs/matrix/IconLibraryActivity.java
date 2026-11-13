package dev.vijaykumarbs.matrix;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

import java.util.ArrayList;

/** Searchable, lazily-bound icon browser. No bitmaps, no libraries. */
public final class IconLibraryActivity extends Activity {

    private IconAdapter adapter;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.edgeToEdge(this);
        setContentView(R.layout.activity_library);
        Ui.padForSystemBars(findViewById(R.id.root));

        adapter = new IconAdapter(this);
        GridView grid = findViewById(R.id.grid);
        grid.setAdapter(adapter);
        empty = findViewById(R.id.empty);

        TextView count = findViewById(R.id.count);
        count.setText(IconCatalog.COUNT + " ICONS");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        EditText search = findViewById(R.id.search);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int visible = adapter.filter(s.toString());
                empty.setVisibility(visible == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
}
