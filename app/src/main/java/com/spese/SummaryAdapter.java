package com.spese;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.spese.db.YearlySummary;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SummaryAdapter extends RecyclerView.Adapter<SummaryAdapter.ViewHolder> {

    private final List<YearlySummary> items = new ArrayList<>();
    private final DecimalFormat euroFormat;

    public SummaryAdapter() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ITALY);
        this.euroFormat = new DecimalFormat("#,##0.00", symbols);
    }

    public void setItems(List<YearlySummary> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        YearlySummary item = items.get(position);
        holder.textAnno.setText(String.valueOf(item.year));
        holder.textTotale.setText("€ " + euroFormat.format(item.total));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textAnno;
        final TextView textTotale;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textAnno = itemView.findViewById(R.id.text_anno);
            textTotale = itemView.findViewById(R.id.text_totale);
        }
    }
}
