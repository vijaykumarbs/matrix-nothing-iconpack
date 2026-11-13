package dev.vijaykumarbs.matrix;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

/** Recycles views and caches drawable constant states, so scrolling stays cheap. */
final class IconAdapter extends BaseAdapter {

    private final Context context;
    private final LayoutInflater inflater;
    private final Drawable.ConstantState[] cache = new Drawable.ConstantState[IconCatalog.COUNT];
    private int[] items;

    IconAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.items = new int[IconCatalog.COUNT];
        for (int i = 0; i < items.length; i++) items[i] = i;
    }

    int filter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            items = new int[IconCatalog.COUNT];
            for (int i = 0; i < items.length; i++) items[i] = i;
        } else {
            ArrayList<Integer> hits = new ArrayList<>(32);
            for (int i = 0; i < IconCatalog.COUNT; i++) {
                if (IconCatalog.LABELS[i].toLowerCase(Locale.ROOT).contains(q)
                        || IconCatalog.CATEGORIES[i].toLowerCase(Locale.ROOT).contains(q)
                        || IconCatalog.NAMES[i].contains(q)) {
                    hits.add(i);
                }
            }
            items = new int[hits.size()];
            for (int i = 0; i < items.length; i++) items[i] = hits.get(i);
        }
        notifyDataSetChanged();
        return items.length;
    }

    @Override
    public int getCount() {
        return items.length;
    }

    @Override
    public Object getItem(int position) {
        return IconCatalog.LABELS[items[position]];
    }

    @Override
    public long getItemId(int position) {
        return items[position];
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_icon, parent, false);
            holder = new Holder();
            holder.tile = convertView.findViewById(R.id.tile);
            holder.label = convertView.findViewById(R.id.label);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        int index = items[position];
        Drawable.ConstantState state = cache[index];
        if (state == null) {
            state = context.getDrawable(IconCatalog.RES[index]).getConstantState();
            cache[index] = state;
        }
        Resources res = context.getResources();
        Drawable drawable = state != null
                ? state.newDrawable(res, context.getTheme())
                : context.getDrawable(IconCatalog.RES[index]);
        holder.tile.setIcon(drawable);
        holder.label.setText(IconCatalog.LABELS[index]);
        return convertView;
    }

    private static final class Holder {
        IconTileView tile;
        TextView label;
    }
}
